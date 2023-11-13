package com.incandescent.woodaengserver.dto.game;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerMatchResponse {
    private String gameCode;
    private String teamP1;
    private String teamP2;
    private String teamP3;
    private String teamB1;
    private String teamB2;
    private String teamB3;

    public PlayerMatchResponse(String gameCode, List<String> teamP, List<String> teamB) {
        this.gameCode = gameCode;
        this.teamP1 = teamP.get(0);
        this.teamP2 = teamP.get(1);
        this.teamP3 = teamP.get(2);
        this.teamB1 = teamB.get(0);
        this.teamB2 = teamB.get(1);
        this.teamB3 = teamB.get(2);
    }
}
