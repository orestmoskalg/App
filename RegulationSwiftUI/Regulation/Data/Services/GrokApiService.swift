import Foundation

/// xAI Grok API: chat completions
struct GrokApiService {
    let apiKey: String
    let baseURL = URL(string: "https://api.x.ai/v1/")!

    func createCompletion(model: String = "grok-4-latest", messages: [[String: String]], maxTokens: Int = 8192, temperature: Double = 0.0) async throws -> String {
        var request = URLRequest(url: baseURL.appendingPathComponent("chat/completions"))
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

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode) else {
            throw GrokApiError.badResponse
        }

        let json = try JSONSerialization.jsonObject(with: data) as? [String: Any]
        guard let choices = json?["choices"] as? [[String: Any]],
              let first = choices.first,
              let message = first["message"] as? [String: Any],
              let content = message["content"] as? String else {
            throw GrokApiError.parseError
        }
        return content
    }
}

enum GrokApiError: Error {
    case badResponse
    case parseError
}
