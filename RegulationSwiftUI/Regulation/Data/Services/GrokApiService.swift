import Foundation
import os.log

// MARK: - Error Types

/// Detailed API errors with context for debugging and user-facing messages
enum GrokApiError: LocalizedError {
    case invalidURL
    case httpError(statusCode: Int, body: String?)
    case rateLimited(retryAfter: TimeInterval?)
    case decodingFailed(underlying: Error)
    case emptyResponse
    case cancelled
    case networkUnavailable
    case timeout

    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "Invalid API URL configuration"
        case .httpError(let code, let body):
            let detail = body.map { " — \($0.prefix(200))" } ?? ""
            return "API returned HTTP \(code)\(detail)"
        case .rateLimited(let retryAfter):
            let wait = retryAfter.map { " Retry after \(Int($0))s." } ?? ""
            return "Rate limited by API.\(wait)"
        case .decodingFailed(let err):
            return "Failed to parse API response: \(err.localizedDescription)"
        case .emptyResponse:
            return "API returned an empty response"
        case .cancelled:
            return "Request was cancelled"
        case .networkUnavailable:
            return "No internet connection"
        case .timeout:
            return "Request timed out. Please try again."
        }
    }

    /// Whether this error is worth retrying
    var isRetryable: Bool {
        switch self {
        case .httpError(let code, _):
            return code == 429 || code == 502 || code == 503 || code == 504
        case .rateLimited:
            return true
        case .timeout, .networkUnavailable:
            return true
        default:
            return false
        }
    }
}

// MARK: - GrokApiService

/// xAI Grok API client with retry, timeout, cancellation, and structured logging.
///
/// Usage:
/// ```swift
/// let service = GrokApiService(apiKey: "xai-...")
/// let response = try await service.createCompletion(messages: [...])
/// ```
struct GrokApiService {
    let apiKey: String
    let baseURL: URL
    let timeout: TimeInterval
    let maxRetries: Int

    private let logger = Logger(subsystem: "com.regulation.assistant", category: "GrokAPI")
    private let session: URLSession

    init(
        apiKey: String,
        baseURL: URL = URL(string: "https://api.x.ai/v1/")!,
        timeout: TimeInterval? = nil,
        maxRetries: Int? = nil
    ) {
        self.apiKey = apiKey
        self.baseURL = baseURL

        let config = ConfigManager.shared
        self.timeout = timeout ?? config.apiTimeout
        self.maxRetries = maxRetries ?? config.maxRetries

        // Dedicated URLSession with timeout
        let sessionConfig = URLSessionConfiguration.default
        sessionConfig.timeoutIntervalForRequest = self.timeout
        sessionConfig.timeoutIntervalForResource = self.timeout * 2
        sessionConfig.waitsForConnectivity = false
        self.session = URLSession(configuration: sessionConfig)
    }

    // MARK: - Public API

    /// Send a chat completion request with automatic retry on transient failures.
    ///
    /// - Parameters:
    ///   - model: The Grok model to use
    ///   - messages: Array of role/content message dictionaries
    ///   - maxTokens: Maximum tokens in response
    ///   - temperature: Sampling temperature (0.0 = deterministic)
    /// - Returns: The assistant's text response
    /// - Throws: `GrokApiError` on failure after all retries exhausted
    func createCompletion(
        model: String = "grok-4-latest",
        messages: [[String: String]],
        maxTokens: Int = 16384,
        temperature: Double = 0.0
    ) async throws -> String {

        let request = try buildRequest(model: model, messages: messages, maxTokens: maxTokens, temperature: temperature)

        var lastError: GrokApiError = .emptyResponse

        for attempt in 0..<maxRetries {
            // Check cancellation before each attempt
            try Task.checkCancellation()

            if attempt > 0 {
                let delay = backoffDelay(attempt: attempt)
                logger.info("Retry \(attempt)/\(self.maxRetries - 1) after \(String(format: "%.1f", delay))s")
                try await Task.sleep(nanoseconds: UInt64(delay * 1_000_000_000))
                try Task.checkCancellation()
            }

            do {
                let result = try await executeRequest(request, attempt: attempt)
                return result
            } catch let error as GrokApiError {
                lastError = error
                if !error.isRetryable {
                    logger.error("Non-retryable error on attempt \(attempt): \(error.localizedDescription)")
                    throw error
                }
                logger.warning("Retryable error on attempt \(attempt): \(error.localizedDescription)")
            } catch is CancellationError {
                throw GrokApiError.cancelled
            } catch let urlError as URLError {
                lastError = mapURLError(urlError)
                if !lastError.isRetryable {
                    throw lastError
                }
                logger.warning("URLError on attempt \(attempt): \(urlError.localizedDescription)")
            }
        }

        logger.error("All \(self.maxRetries) attempts exhausted")
        throw lastError
    }

    // MARK: - Private Helpers

    private func buildRequest(
        model: String,
        messages: [[String: String]],
        maxTokens: Int,
        temperature: Double
    ) throws -> URLRequest {
        let url = baseURL.appendingPathComponent("chat/completions")

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let body: [String: Any] = [
            "model": model,
            "messages": messages,
            "max_tokens": maxTokens,
            "temperature": temperature
        ]

        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        return request
    }

    private func executeRequest(_ request: URLRequest, attempt: Int) async throws -> String {
        let startTime = CFAbsoluteTimeGetCurrent()

        let (data, response) = try await session.data(for: request)

        let elapsed = CFAbsoluteTimeGetCurrent() - startTime
        logger.debug("API call completed in \(String(format: "%.2f", elapsed))s (attempt \(attempt))")

        guard let http = response as? HTTPURLResponse else {
            throw GrokApiError.httpError(statusCode: 0, body: "Non-HTTP response")
        }

        // Handle rate limiting specifically
        if http.statusCode == 429 {
            let retryAfter = http.value(forHTTPHeaderField: "Retry-After")
                .flatMap { TimeInterval($0) }
            throw GrokApiError.rateLimited(retryAfter: retryAfter)
        }

        // Handle other non-success codes
        guard (200...299).contains(http.statusCode) else {
            let bodyStr = String(data: data, encoding: .utf8)
            throw GrokApiError.httpError(statusCode: http.statusCode, body: bodyStr)
        }

        // Parse response
        guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let choices = json["choices"] as? [[String: Any]],
              let first = choices.first,
              let message = first["message"] as? [String: Any],
              let content = message["content"] as? String,
              !content.isEmpty else {
            throw GrokApiError.emptyResponse
        }

        logger.info("Received \(content.count) chars from API")
        return content
    }

    /// Exponential backoff: 1s, 2s, 4s, capped at 10s
    private func backoffDelay(attempt: Int) -> TimeInterval {
        min(pow(2.0, Double(attempt)), 10.0)
    }

    private func mapURLError(_ error: URLError) -> GrokApiError {
        switch error.code {
        case .timedOut:
            return .timeout
        case .notConnectedToInternet, .networkConnectionLost:
            return .networkUnavailable
        case .cancelled:
            return .cancelled
        default:
            return .httpError(statusCode: error.code.rawValue, body: error.localizedDescription)
        }
    }
}
