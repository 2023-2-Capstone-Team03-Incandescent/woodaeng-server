package com.incandescent.woodaengserver.controller;

import com.incandescent.woodaengserver.dto.game.GamePlayRequest;
import com.incandescent.woodaengserver.dto.game.GameReadyRequest;
import com.incandescent.woodaengserver.dto.game.PlayerMatchRequest;
import com.incandescent.woodaengserver.service.GameMatchingService;
import com.incandescent.woodaengserver.service.GamePlayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class GameController {
    private final GameMatchingService gameMatchingService;
    private final GamePlayService gamePlayService;

    @MessageMapping("/game/matching")
    public void matching(@Payload PlayerMatchRequest playerMatchRequest) {
        gameMatchingService.joinLocationQueue(playerMatchRequest.getId(), playerMatchRequest.getLatitude(), playerMatchRequest.getLongitude());
    }

    @MessageMapping("/game/ready/{gameCode}")
    public void ready(@DestinationVariable String gameCode, @Payload GameReadyRequest gameReadyRequest) {
        String id = gameReadyRequest.getId();
        int team = gameReadyRequest.getTeam();

        gamePlayService.readyGame(gameCode, id, team);
    }

    @MessageMapping("/game/play/{gameCode}")
    public void play(@DestinationVariable String gameCode, @Payload GamePlayRequest gamePlayRequest) {
        gamePlayService.playGame(gameCode, gamePlayRequest);
    }
}