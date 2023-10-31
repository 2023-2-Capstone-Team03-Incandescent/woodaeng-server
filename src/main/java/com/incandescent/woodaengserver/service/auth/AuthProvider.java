package com.incandescent.woodaengserver.service.auth;

import com.incandescent.woodaengserver.repository.AuthRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;

import org.springframework.stereotype.Service;

@Service
public class AuthProvider {
    private final AuthRepository authRepository;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    @Autowired
    public AuthProvider(AuthRepository authRepository, AuthenticationManagerBuilder authenticationManagerBuilder) {
        this.authRepository = authRepository;
        this.authenticationManagerBuilder = authenticationManagerBuilder;
    }

    public boolean isRefreshTokenEqual(String token) {
        if (!authRepository.checkRefreshToken(token))
            return false;

        return true;
    }

}