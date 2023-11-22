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
    @OneToOne
    @JoinColumn(referencedColumnName = "id")
    private Long user_id;
    private double latitude;
    private double longitude;
    private int team;
    @ManyToOne
    @JoinColumn(referencedColumnName = "game_code")
    private String game_code;
    private int ball_cnt;
    private int gold_cnt;
    private int box_cnt;
    private int mini_cnt;
}