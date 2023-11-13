package com.incandescent.woodaengserver.config;

import com.incandescent.woodaengserver.service.auth.AuthService;
import com.incandescent.woodaengserver.service.auth.PrincipalOAuth2DetailsService;
import com.incandescent.woodaengserver.service.auth.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity (debug = true)
public class SecurityConfig {

    private final PrincipalOAuth2DetailsService principalOAuth2DetailsService;
    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;


    @Autowired
    public SecurityConfig(PrincipalOAuth2DetailsService principalOAuth2DetailsService, AuthService authService, JwtTokenProvider jwtTokenProvider) {
        this.principalOAuth2DetailsService = principalOAuth2DetailsService;
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
//		return new BCryptPasswordEncoder();
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement((sessionManagement) -> sessionManagement
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling((exceptionConfig) -> exceptionConfig
                        .authenticationEntryPoint(new CustomAuthenticationEntryPoint())
                        .accessDeniedHandler(new CustomAccessDeniedHandler()))
                .oauth2Login((oauth2) -> oauth2
                        .userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint
                                .userService(principalOAuth2DetailsService))
                        .successHandler(new OAuth2SuccessHandler(jwtTokenProvider,authService)));
        return http.build();
    }
}