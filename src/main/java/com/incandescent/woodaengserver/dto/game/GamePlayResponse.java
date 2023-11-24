package com.incandescent.woodaengserver.dto.game;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GamePlayResponse {
    private Long id;
    private int team;
    private int ballId1;
    private int ballId2;
    private int teamRScore;
    private int teamBScore;
    private int random;
}
