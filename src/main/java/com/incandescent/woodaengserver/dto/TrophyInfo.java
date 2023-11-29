package com.incandescent.woodaengserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrophyInfo {
    private Long user_id;
    private int ball_cnt;
    private int gold_cnt;
    private int box_cnt;
    private int mini_cnt;
    private int game_cnt;
    private int win_cnt;
    private int mvp_cnt;
}
