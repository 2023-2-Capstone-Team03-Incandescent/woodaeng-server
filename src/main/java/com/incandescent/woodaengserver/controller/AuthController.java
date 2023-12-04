package com.incandescent.woodaengserver.controller;

import com.incandescent.woodaengserver.dto.*;
import com.incandescent.woodaengserver.service.UserProvider;
import com.incandescent.woodaengserver.service.auth.AuthService;
import com.incandescent.woodaengserver.service.auth.JwtTokenProvider;
import com.incandescent.woodaengserver.service.UserService;
import com.incandescent.woodaengserver.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;
    private final UserProvider userProvider;
    private final AuthService authService;

    @Autowired
    @Lazy
    public AuthController(PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider, UserService userService, UserProvider userProvider, AuthService authService) {
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService;
        this.userProvider = userProvider;
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity join(@RequestBody UserSignupRequest postUserReq) {
        String encodedPassword = passwordEncoder.encode(postUserReq.getPassword());
        User user = new User(postUserReq.getEmail(), postUserReq.getNickname(), encodedPassword, "ROLE_USER", "none", "none");
        try {
            userService.createUser(user);
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); //409
        }
    }

    @PostMapping("/signin")
    public ResponseEntity login(@RequestBody UserSigninRequest userSigninRequest) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userSigninRequest.getEmail(), userSigninRequest.getPassword());
        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        PrincipalDetails userEntity = (PrincipalDetails) authentication.getPrincipal();

        Long id = userEntity.getUser().getId();
        String accessToken = jwtTokenProvider.createAccessToken(id);
        String refreshToken = jwtTokenProvider.createRefreshToken(id);

        authService.registerRefreshToken(id, refreshToken);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        return ResponseEntity.status(HttpStatus.OK)
                .headers(headers)
                .body(new UserSigninResponse(accessToken, refreshToken));
    }

    @PostMapping("/signin/auto")
    public ResponseEntity relogin(@RequestBody UserReSigninRequest userReSigninRequest) throws Exception {
        String accessToken = userReSigninRequest.getAccessToken();

        try {
            if(jwtTokenProvider.getExpiration(accessToken) > 0)
                return ResponseEntity.status(HttpStatus.OK).build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    @PostMapping("/signin/auto/newtk")
    public ResponseEntity reissue(@RequestBody UserReSigninNewTokenRequest userReSigninNewTokenRequest) throws Exception {
        String accessToken = userReSigninNewTokenRequest.getAccessToken();
        String refreshToken = userReSigninNewTokenRequest.getRefreshToken();

        try {
            if (jwtTokenProvider.getExpiration(accessToken) > 0)
                return ResponseEntity.status(HttpStatus.OK).build();
            else if (jwtTokenProvider.getExpiration(refreshToken) > 0) {
                Long id = jwtTokenProvider.getUseridFromRef(refreshToken);
                if (userProvider.retrieveById(id) != null) {
                    String newAccessToken = jwtTokenProvider.createAccessToken(id);

                    HttpHeaders headers = new HttpHeaders();
                    headers.add("Content-Type", "application/json");

                    return ResponseEntity.status(HttpStatus.OK)
                            .headers(headers)
                            .body(new UserSigninResponse(newAccessToken, refreshToken));
                } else
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    @GetMapping("/oauth2/success/*")
    public ResponseEntity loginSuccess(@RequestParam("accessToken") String accessToken, @RequestParam("refreshToken") String refreshToken) {
        UserSigninResponse postLoginRes = new UserSigninResponse(accessToken, refreshToken);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        return ResponseEntity.status(HttpStatus.OK)
                .headers(headers)
                .body(postLoginRes);
    }
}