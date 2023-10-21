package com.incandescent.woodaengserver.auth;

import com.incandescent.woodaengserver.auth.AuthService;
import com.incandescent.woodaengserver.auth.dto.*;
import com.incandescent.woodaengserver.auth.jwt.JwtTokenProvider;
import com.incandescent.woodaengserver.auth.PrincipalDetails;
import com.incandescent.woodaengserver.user.UserService;
import com.incandescent.woodaengserver.user.model.User;
import com.incandescent.woodaengserver.util.BaseException;
import com.incandescent.woodaengserver.util.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final AuthService authService;

    @Autowired
    public AuthController(PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider, UserService userService, AuthService authService) {
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService;
        this.authService = authService;
    }

    @PostMapping("/signup")
    public BaseResponse<String> join(@RequestBody PostSignupReq postUserReq) throws BaseException {

        String encodedPassword = passwordEncoder.encode(postUserReq.getPassword());
        User user = new User(postUserReq.getEmail(), postUserReq.getNickname(), encodedPassword, "ROLE_USER", "none", "none");
        try {
            userService.createUser(user);
            return new BaseResponse("회원가입 성공");
        } catch (BaseException e) {
            return new BaseResponse(e.getStatus());
        }
    }

    @PostMapping("/signin")
    public BaseResponse<PostSigninAutoRes> login(@RequestBody PostSigninAutoReq postLoginReq) {

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(postLoginReq.getEmail(), postLoginReq.getPassword());
        Authentication authentication = authenticationManager.authenticate(authenticationToken);
        log.info("유저 인증 성공. 자동 로그인");

        PrincipalDetails userEntity = (PrincipalDetails) authentication.getPrincipal();
        System.out.println(userEntity);

        Long user_id = userEntity.getUser().getId();
        String accessToken = jwtTokenProvider.createAccessToken(user_id);
        String refreshToken = jwtTokenProvider.createRefreshToken(user_id);

        authService.registerRefreshToken(user_id, refreshToken);
        return new BaseResponse<>(new PostSigninAutoRes(accessToken, refreshToken));
    }

    @GetMapping("/oauth2/success")
    public BaseResponse<PostSigninAutoRes> loginSuccess(@RequestParam("accessToken") String accessToken, @RequestParam("refreshToken") String refreshToken) {
        PostSigninAutoRes postLoginRes = new PostSigninAutoRes(accessToken, refreshToken);
        return new BaseResponse<>(postLoginRes);
    }

}