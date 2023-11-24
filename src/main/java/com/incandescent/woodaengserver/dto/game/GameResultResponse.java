package com.incandescent.woodaengserver.dto.game;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GameResultResponse {
    private int team;
    private int teamRScore;
    private int teamBScore;
    private List<GamePlayerResult> playerResults;
}
