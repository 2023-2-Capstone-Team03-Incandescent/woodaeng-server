package com.incandescent.woodaengserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserReSigninNewTokenRequest {
    private String accessToken;
    private String refreshToken;
}