package com.incandescent.woodaengserver.domain;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player {
    private Long user_id;
    private double latitude;
    private double longitude;
    private int team;
    private String game_code;
    private int ball_cnt;
    private int gold_cnt;
    private int box_cnt;
    private int mini_cnt;

    public Player(Long playerId, double latitude, double longitude, int team, String gameCode) {
        this.user_id = playerId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.team = team;
        this.game_code = gameCode;
    }
}