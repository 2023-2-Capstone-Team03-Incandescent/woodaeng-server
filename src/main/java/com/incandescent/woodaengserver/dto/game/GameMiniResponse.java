package com.incandescent.woodaengserver.dto.game;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameMiniResponse {
    private String startTime;
    private Long id;
    private String myImage;
    private String myNick;
    private int myTeam;
    private int gameType;
    private Long opponentId;
    private String opponentImage;
    private String opponentNick;
    private String question;
    private HashMap<Integer, String> options;
    private int answer;
}
