package com.incandescent.woodaengserver.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameRecord {
    private Long user_id;
    private String game_code;
    private int mvp;
    private int win;
    private String location;
    private int ball_cnt;
    private Timestamp time;
}
