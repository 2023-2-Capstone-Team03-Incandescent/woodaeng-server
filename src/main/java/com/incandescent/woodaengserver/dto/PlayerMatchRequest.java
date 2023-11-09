package com.incandescent.woodaengserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerMatchRequest {
    private Long id;
    private String nickname;
    private double latitude;
    private double longitude;
    private int team;
    private Long game_code;
}