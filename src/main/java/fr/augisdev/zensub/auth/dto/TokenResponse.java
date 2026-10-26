package fr.augisdev.zensub.auth.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
