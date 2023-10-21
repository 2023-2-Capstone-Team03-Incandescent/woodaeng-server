package com.incandescent.woodaengserver.auth;

import com.incandescent.woodaengserver.auth.AuthService;
import com.incandescent.woodaengserver.auth.oauth.PrincipalOAuth2DetailsService;
import com.incandescent.woodaengserver.auth.jwt.JwtTokenProvider;
import com.incandescent.woodaengserver.auth.jwt.config.CustomAccessDeniedHandler;
import com.incandescent.woodaengserver.auth.jwt.config.CustomAuthenticationEntryPoint;
import com.incandescent.woodaengserver.auth.jwt.config.OAuth2SuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
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
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)

                .and()

                .exceptionHandling()
                .authenticationEntryPoint(new CustomAuthenticationEntryPoint())
                .accessDeniedHandler(new CustomAccessDeniedHandler())

                .and()

                .oauth2Login()
                .userInfoEndpoint()
                .userService(principalOAuth2DetailsService)

                .and()

                .successHandler(new OAuth2SuccessHandler(jwtTokenProvider,authService));

        return http.build();
    }
}