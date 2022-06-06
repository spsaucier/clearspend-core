package com.clearspend.capital.controller.type.twofactor;

public record TwoFactorStartResponse(String twoFactorId, String methodId, String trustChallenge) {}
