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
    private List<Long> teamRed;
    private List<Long> teamBlue;
    private List<BallLocation> balls;
}
