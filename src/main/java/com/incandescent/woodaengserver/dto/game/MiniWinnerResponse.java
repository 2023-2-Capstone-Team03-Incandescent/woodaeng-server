package com.incandescent.woodaengserver.dto.game;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniWinnerResponse {
    private Long winnerId;
    private String winnerDog;
    private int ballId1;
    private int ballId2;
}
