package com.incandescent.woodaengserver.service.auth;

import com.incandescent.woodaengserver.repository.AuthRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthRepository authRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public AuthService(AuthRepository authRepository, JwtTokenProvider jwtTokenProvider) {
        this.authRepository = authRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public Long registerRefreshToken(Long userid, String refreshToken) {
        if(authRepository.checkUser(userid))
            authRepository.updateRefreshToken(userid, refreshToken);
        else
            authRepository.insertRefreshToken(userid,refreshToken);

        return userid;
    }

    public Long modifyRefreshToken(Long userid, String refreshToken) {
        this.authRepository.updateRefreshToken(userid,refreshToken);

        return userid;
    }


}