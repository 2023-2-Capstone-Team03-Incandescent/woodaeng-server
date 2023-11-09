package com.incandescent.woodaengserver.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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