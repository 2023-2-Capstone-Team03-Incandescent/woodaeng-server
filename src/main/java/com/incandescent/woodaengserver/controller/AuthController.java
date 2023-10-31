package com.incandescent.woodaengserver.controller;

import com.incandescent.woodaengserver.dto.*;
import com.incandescent.woodaengserver.service.UserProvider;
import com.incandescent.woodaengserver.service.auth.AuthService;
import com.incandescent.woodaengserver.service.auth.JwtTokenProvider;
import com.incandescent.woodaengserver.service.UserService;
import com.incandescent.woodaengserver.domain.User;
import com.incandescent.woodaengserver.util.CustomException;
import com.incandescent.woodaengserver.util.ResponseStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
            return ResponseEntity.ok().build();
        } catch (CustomException e) {
            return ResponseEntity.badRequest().body(e.getStatus());
        }
    }

    @PostMapping("/signin")
    public ResponseEntity login(@RequestBody UserSigninRequest postLoginReq) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(postLoginReq.getEmail(), postLoginReq.getPassword());
        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        PrincipalDetails userEntity = (PrincipalDetails) authentication.getPrincipal();

        Long id = userEntity.getUser().getId();
        String accessToken = jwtTokenProvider.createAccessToken(id);
        String refreshToken = jwtTokenProvider.createRefreshToken(id);

        authService.registerRefreshToken(id, refreshToken);
        return ResponseEntity.ok(new UserSigninResponse(accessToken, refreshToken));
    }

    @PostMapping("/signin/re")
    public ResponseEntity relogin(@RequestBody UserReSigninRequest userReSigninRequest) throws CustomException {
        String accessToken = userReSigninRequest.getAccessToken();
        String refreshToken = userReSigninRequest.getRefreshToken();

        if(jwtTokenProvider.getExpiration(accessToken) > 0) {
            return ResponseEntity.ok(new UserSigninResponse(accessToken, refreshToken));
        }
        else if (jwtTokenProvider.getExpiration(refreshToken) > 0) {
            Long id = jwtTokenProvider.getUseridFromRef(refreshToken);
            try {
                if(userProvider.retrieveById(id) != null) {
                    String newAccessToken = jwtTokenProvider.createAccessToken(id);
                    return ResponseEntity.ok(new UserSigninResponse(newAccessToken, refreshToken));
                }
            } catch (CustomException e) {
                return ResponseEntity.notFound().build();
            }
        }
        throw new CustomException(ResponseStatus.EXPIRED_JWT);
    }

    @GetMapping("/oauth2/success")
    public ResponseEntity loginSuccess(@RequestParam("accessToken") String accessToken, @RequestParam("refreshToken") String refreshToken) {
        UserSigninResponse postLoginRes = new UserSigninResponse(accessToken, refreshToken);
        return ResponseEntity.ok(postLoginRes);
    }
}