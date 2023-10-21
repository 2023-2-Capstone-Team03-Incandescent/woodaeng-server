package com.incandescent.woodaengserver.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetUserRes {
    private String nickname;
    private String introduce;
}