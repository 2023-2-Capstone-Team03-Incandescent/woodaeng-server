package com.incandescent.woodaengserver.dto.game;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GamePlayerResult {
    private Long id;
    private int team;
    private String nickname;
    private String image;
    private int ball_cnt;
    private int gold_cnt;
    private int box_cnt;
    private int mini_cnt;
}
