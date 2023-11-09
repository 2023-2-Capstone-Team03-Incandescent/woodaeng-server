package com.incandescent.woodaengserver.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player {
    private Long id;
    private String nickname;
    private double latitude;
    private double longitude;
    private int team;
    private Long game_code;
}