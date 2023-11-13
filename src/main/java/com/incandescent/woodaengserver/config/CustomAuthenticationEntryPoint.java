package com.incandescent.woodaengserver.config;

import jakarta.servlet.ServletException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        String errorType = "Authentication failed";

        // BadCredentialsException
        if(authException instanceof BadCredentialsException){
            errorType = "password";
        }

        // UsernameNotFoundException
        if(authException instanceof UsernameNotFoundException){
            errorType = "username";
        }

        log.info("Authentication failed : " + errorType);

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"Authentication failed\", \"type\": \"" + errorType + "\"}");
    }
}