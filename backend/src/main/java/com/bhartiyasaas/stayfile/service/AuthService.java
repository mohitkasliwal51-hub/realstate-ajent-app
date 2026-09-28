package com.bhartiyasaas.stayfile.service;

import com.bhartiyasaas.stayfile.dto.request.LoginRequest;
import com.bhartiyasaas.stayfile.dto.request.RegisterRequest;
import com.bhartiyasaas.stayfile.dto.response.AuthResponse;
import com.bhartiyasaas.stayfile.dto.response.UserProfileResponse;
import com.bhartiyasaas.stayfile.security.SecurityUser;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserProfileResponse getCurrentUserProfile(SecurityUser currentUser);
}
