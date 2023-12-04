package com.incandescent.woodaengserver.config;

import com.incandescent.woodaengserver.service.auth.AuthService;
import com.incandescent.woodaengserver.service.auth.JwtTokenProvider;
import com.incandescent.woodaengserver.dto.PrincipalDetails;
import jakarta.servlet.ServletException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
@Component
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthService authService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        log.info("OAuth Success");
        PrincipalDetails oAuth2User = (PrincipalDetails) authentication.getPrincipal();
        String targetUrl;
        String accessToken =  jwtTokenProvider.createAccessToken(oAuth2User.getUser().getId());
        String refreshToken =  jwtTokenProvider.createRefreshToken(oAuth2User.getUser().getId());

        authService.registerRefreshToken(oAuth2User.getUser().getId(),refreshToken);

//        targetUrl = UriComponentsBuilder.fromUriString("/auth/oauth2/success")
//                .queryParam("accessToken",accessToken)
//                .queryParam("refreshToken",refreshToken)
//                .build().toUriString();
//        getRedirectStrategy().sendRedirect(request, response, targetUrl);

        String redirectUri = "didyouseemydog://success" +
                "?access-token=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8.toString()) +
                "&refresh-token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8.toString());

        response.setStatus(HttpStatus.OK.value());
        response.setHeader("Location", redirectUri);
    }
}