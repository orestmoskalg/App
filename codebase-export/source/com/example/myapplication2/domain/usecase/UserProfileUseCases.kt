package com.example.myapplication2.domain.usecase

import com.example.myapplication2.domain.model.UserProfile
import com.example.myapplication2.domain.repository.UserProfileRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveUserProfileUseCase @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
) {
    operator fun invoke(): Flow<UserProfile?> = userProfileRepository.observeUserProfile()
}

class SaveUserProfileUseCase @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
) {
    suspend operator fun invoke(profile: UserProfile) {
        userProfileRepository.saveUserProfile(profile)
    }
}
