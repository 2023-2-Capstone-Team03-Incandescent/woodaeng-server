package com.incandescent.woodaengserver.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.incandescent.woodaengserver.domain.Player;
import com.incandescent.woodaengserver.dto.game.*;
import com.incandescent.woodaengserver.repository.GameRepository;
import com.incandescent.woodaengserver.service.GameMatchingService;
import com.incandescent.woodaengserver.service.GamePlayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.parser.ParseException;
import org.quartz.SchedulerException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class GameController {
    private final GameMatchingService gameMatchingService;
    private final GamePlayService gamePlayService;

    @MessageMapping("/game/matching")
    public void matching(@Payload PlayerMatchRequest playerMatchRequest) throws IOException {
        gameMatchingService.joinLocationQueue(playerMatchRequest.getId(), playerMatchRequest.getLatitude(), playerMatchRequest.getLongitude());

    }

    @MessageMapping("/game/ready/{gameCode}")
    public void ready(@DestinationVariable String gameCode, @Payload GameReadyRequest gameReadyRequest) throws JsonProcessingException, SchedulerException {
        Long id = gameReadyRequest.getId();
        int team = gameReadyRequest.getTeam();

        gamePlayService.readyGame(gameCode, id, team);
    }

    @MessageMapping("/game/play/{gameCode}")
    public void play(@DestinationVariable String gameCode, @Payload GamePlayRequest gamePlayRequest) throws JsonProcessingException {
        gamePlayService.playGame(gameCode, gamePlayRequest);
    }

    @MessageMapping("/game/location/{gameCode}")
    public void location(@DestinationVariable String gameCode, @Payload PlayerMatchRequest playerMatchRequest) throws JsonProcessingException {
        gamePlayService.realTimeLocation(gameCode, playerMatchRequest);
    }

    @MessageMapping("/game/mini/{gameCode}")
    public void mini(@DestinationVariable String gameCode, @Payload PlayerMiniWinner playerMiniWinner) throws JsonProcessingException {
        gamePlayService.miniResult(gameCode, playerMiniWinner);
    }
}