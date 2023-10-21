package com.incandescent.woodaengserver.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostSigninAutoRes {
    private String accessToken;
    private String refreshToken;
}