package com.incandescent.woodaengserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.incandescent.woodaengserver.dto.game.*;
import com.incandescent.woodaengserver.repository.GameRepository;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.listener.PatternTopic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Service
public class GamePlayService {

    private final RedisTemplate<String, String> redisTemplate;
    private static RedisMessageListenerContainer container = null;
    private static SimpMessagingTemplate messagingTemplate = null;
    private final RedisPublisher redisPublisher;
    private final RedisSubscriber redisSubscriber;
    private static int goldNum = 20;
    private static int randNum = 30;
    private static GameRepository gameRepository = null;

    @Autowired
    public GamePlayService(RedisMessageListenerContainer container, RedisTemplate<String, String> redisTemplate, SimpMessagingTemplate messagingTemplate, RedisPublisher redisPublisher, RedisSubscriber redisSubscriber, GameRepository gameRepository) {
        this.redisTemplate = redisTemplate;
        this.container = container;
        this.messagingTemplate = messagingTemplate;
        this.redisPublisher = redisPublisher;
        this.redisSubscriber = redisSubscriber;
        this.gameRepository = gameRepository;
    }

    public synchronized void subscribeToRedis(String topic) {
        redisSubscriber.setTopic(topic);
        container.addMessageListener(redisSubscriber, new PatternTopic(topic));
    }

    public static synchronized void unsubscribeFromRedis() {
        container.stop();
    }

    public synchronized void readyGame(String gameCode, Long id, int team) throws JsonProcessingException, SchedulerException {
        LocalDateTime startTime = LocalDateTime.now().plusSeconds(5);
        LocalDateTime endTime = startTime.plusSeconds(150); // 150초

        GameReadyResponse gameReadyResponse = new GameReadyResponse(startTime.toString());
        String jsonGameReadyResponse = new ObjectMapper().writeValueAsString(gameReadyResponse);
        messagingTemplate.convertAndSend("/topic/game/ready/" + gameCode, jsonGameReadyResponse);

        subscribeToRedis("/game/play/" + gameCode);

        JobDetail endjob = JobBuilder.newJob(endJob.class)
                .usingJobData("gameCode", gameCode)
                .withIdentity("endJob_" + gameCode, "group1")
                .build();
        Trigger endtrigger = TriggerBuilder.newTrigger()
                .withIdentity("endTrigger_" + gameCode, "group1")
                .startAt(java.sql.Timestamp.valueOf(endTime))
                .build();
        Scheduler scheduler = new StdSchedulerFactory().getScheduler();
        scheduler.start();
        scheduler.scheduleJob(endjob, endtrigger);


        LocalDateTime tempTime = startTime;

        while (true) {
//            int randomTime = (int) (Math.random() * 3) + 3; // 3 ~ 5분
//            tempTime.plusMinutes(randomTime);
            int randomTime = (int) (Math.random() * 30) + 10; //10 ~ 40초
            tempTime = tempTime.plusSeconds(randomTime);

            if(tempTime.isAfter(endTime))
                break;

            JobDetail randomJob = JobBuilder.newJob(RandomJob.class)
                    .usingJobData("gameCode", gameCode)
                    .withIdentity("randomJob_" + tempTime, "group1")
                    .build();
            Trigger randomTrigger = TriggerBuilder.newTrigger()
                    .withIdentity("randomTrigger_" + tempTime, "group1")
                    .startAt(java.sql.Timestamp.valueOf(tempTime))
                    .build();
            scheduler.scheduleJob(randomJob, randomTrigger);
            log.info("schedule add");
        }
    }

    public void playGame(String gameCode, GamePlayRequest gamePlayRequest) throws JsonProcessingException {
        GamePlayResponse gamePlayResponse = null;
        if (gamePlayRequest.getBallId() >= 30) { // 랜덤박스
            int color = 0;
            int redScore = gameRepository.selectRedScore(gameCode);
            log.info(String.valueOf(redScore));

            int random = (int) (Math.random() * 3) + 2;
            switch (random) {
                case 2: //1개+
                    List<Integer> balls = gameRepository.selectOurBall(gameCode, gamePlayRequest.getTeam() == 0 ? 1 : 0);
                    int ball = balls.get((int) (Math.random() * balls.size()));

                    if (gameRepository.selectTeam(gamePlayRequest.getId()) == 1) {
                        color = 1;
                        gameRepository.updateBallColor(gameCode, gamePlayRequest.getId(), ball, color, --redScore);
                    } else {
                        gameRepository.updateBallColor(gameCode, gamePlayRequest.getId(), ball, color, ++redScore);
                    }
                    gamePlayResponse = new GamePlayResponse(gamePlayRequest.getId(), gamePlayRequest.getTeam(), ball, 40, redScore, 20-redScore, random);
                    break;
                case 3: //1개-
                    balls = gameRepository.selectOurBall(gameCode, gamePlayRequest.getTeam());
                    ball = balls.get((int) (Math.random() * balls.size()));

                    if (gameRepository.selectTeam(gamePlayRequest.getId()) == 0) {
                        color = 1;
                        gameRepository.updateBallColor(gameCode, gamePlayRequest.getId(), ball, color, --redScore);
                    } else {
                        gameRepository.updateBallColor(gameCode, gamePlayRequest.getId(), ball, color, ++redScore);
                    }
                    gamePlayResponse = new GamePlayResponse(gamePlayRequest.getId(), gamePlayRequest.getTeam(), ball, 40, redScore, 20-redScore, random);
                    break;
                case 4: //안개
                    gamePlayResponse = new GamePlayResponse(gamePlayRequest.getId(), gamePlayRequest.getTeam(), 40, 40, redScore, 20-redScore, random);
                    break;
                case 5: // 포인트
                    gamePlayResponse = new GamePlayResponse(gamePlayRequest.getId(), gamePlayRequest.getTeam(), 40, 40, redScore, 20-redScore, random);
                    // 포인트 추가 로직
                    break;
                case 6: // 꽝
                    gamePlayResponse = new GamePlayResponse(gamePlayRequest.getId(), gamePlayRequest.getTeam(), 40, 40, redScore, 20-redScore, random);

            };
            gameRepository.updateRBNum(gamePlayRequest.getId());
        } else if (gamePlayRequest.getBallId() >= 20) { // 황금원판
            List<Integer> balls = gameRepository.selectOurBall(gameCode, gamePlayRequest.getTeam() == 0 ? 1 : 0);
            int ball1 = balls.get((int) (Math.random() * balls.size()));
            int ball2 = balls.get((int) (Math.random() * balls.size()));
            while (ball1 == ball2)
                ball2 = balls.get((int) (Math.random() * balls.size()));

            int redScore = gameRepository.selectRedScore(gameCode);
            log.info(String.valueOf(redScore));

            int color = 1;
            if (gameRepository.selectTeam(gamePlayRequest.getId()) == 0) {
                color = 0;
                gameRepository.updateBallColor(gameCode, gamePlayRequest.getId(), ball1, color, ++redScore);
                gameRepository.updateBallColor(gameCode, gamePlayRequest.getId(), ball2, color, ++redScore);
            } else {
                gameRepository.updateBallColor(gameCode, gamePlayRequest.getId(), ball1, color, --redScore);
                gameRepository.updateBallColor(gameCode, gamePlayRequest.getId(), ball2, color, --redScore);
            }
            gameRepository.updateGoldNum(gamePlayRequest.getId());

            gamePlayResponse = new GamePlayResponse(gamePlayRequest.getId(), gamePlayRequest.getTeam(), ball1, ball2, redScore, 20-redScore, 1);
        } else { //일반원판
            int color = 0;
            int redScore = gameRepository.selectRedScore(gameCode);
            log.info(String.valueOf(redScore));
            if (gameRepository.selectBallColor(gameCode, gamePlayRequest.getBallId()) == 0) {
                color = 1;
                redScore--;
            } else {
                redScore++;
            }
            gamePlayResponse = new GamePlayResponse(gamePlayRequest.getId(), gamePlayRequest.getTeam(), gamePlayRequest.getBallId(), 40, redScore, 20-redScore, 0);
            gameRepository.updateBallColor(gameCode, gamePlayRequest.getId(), gamePlayRequest.getBallId(), color, redScore);
        }



        ObjectMapper objectMapper = new ObjectMapper();
        String jsonGamePlayResponse = objectMapper.writeValueAsString(gamePlayResponse);
        messagingTemplate.convertAndSend("/topic/game/play/" + gameCode, jsonGamePlayResponse);
//        redisPublisher.publishGameEvent(gameCode, jsonGamePlayResponse);
    }

    public void realTimeLocation(String gameCode, GameReadyRequest gameReadyRequest) throws JsonProcessingException {
        // 다른 사용자와 만나면 전송. 실시간 위치 처리 로직


        Long opponentId = 0L; // 상대방

        int gameType = (int) Math.round(Math.random());
        String question = null;
        HashMap<Integer, String> options = null;
        int answer = 0;

        switch (gameType) {
            case 0: //댕댕퀴즈
                question = "문제";
                options = new HashMap<>() {{
                    put(1, "답안1");
                    put(2, "답안2");
                    put(3, "답안3");
                }};
                answer = 0; //정답
                break;
            case 1: //댕기자랑
                question = "명렁어";
                break;
        }

        GameMiniResponse gameMiniResponse = new GameMiniResponse(gameReadyRequest.getId(), gameType, opponentId, question, options, answer);
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonGameLocationResponse = objectMapper.writeValueAsString(gameMiniResponse);
        messagingTemplate.convertAndSend("/topic/game/location/" + gameCode, jsonGameLocationResponse);
    }

    public static class endJob implements Job {
        public endJob() {}

        @SneakyThrows
        @Override
        public void execute(JobExecutionContext context) {
            unsubscribeFromRedis();

            String gameCode = context.getJobDetail().getJobDataMap().getString("gameCode");
            GameResultResponse gameResultResponse = gameRepository.selectResult(gameCode);
            String jsonGameResultResponse = new ObjectMapper().writeValueAsString(gameResultResponse);

            messagingTemplate.convertAndSend("/topic/game/end/" + gameCode, jsonGameResultResponse);
            log.info("end JOB!!!!!!");
        }
    }

    public static class RandomJob implements Job {
        private String appKey = "3tXkLvjCRz5Hmeb4YZjm9ipPsPkstQ74g9R3SZ5h";

        public RandomJob() {}

        @SneakyThrows
        @Override
        public void execute(JobExecutionContext context) {
            String gameCode = context.getJobDetail().getJobDataMap().getString("gameCode");
            int goldRan = (int) Math.round(Math.random());

            URL locationUrl = new URL("https://apis.openapi.sk.com/tmap/geofencing/regions/"+gameCode.substring(0, gameCode.length()-2)+"?version=1");
            HttpURLConnection conn = (HttpURLConnection) locationUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("appKey", appKey);
            conn.setDoOutput(true);

            String jsonString = "";

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                jsonString = response.toString();
            } catch (Exception e) {
                log.error(e.getMessage());
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            JsonNode arrayNode = jsonNode.get("features");
            JsonNode featuresJson = arrayNode.get(0);
            JsonNode geometryJson = featuresJson.get("geometry");
            ArrayNode coordinatesJson = (ArrayNode) geometryJson.get("coordinates");
            JsonNode coordinatesList = coordinatesJson.get(0);

            List<List<Double>> coorRes = new ArrayList<>();
            for (JsonNode coordinate : coordinatesList) {
                if (coordinate.isArray() && coordinate.size() >= 2) {
                    double longitude = coordinate.get(0).asDouble();
                    double latitude = coordinate.get(1).asDouble();
                    coorRes.add(Arrays.asList(longitude, latitude));
                }
            }

            URL routeUrl = new URL("https://apis.openapi.sk.com/tmap/routes/pedestrian?version=1");
            HttpURLConnection conn2 = (HttpURLConnection) routeUrl.openConnection();
            conn2.setRequestMethod("POST");
            conn2.setRequestProperty("appKey", appKey);
            conn2.setRequestProperty("Content-Type", "application/json");
            conn2.setDoOutput(true);

            int startNum = (int) (Math.random() * coordinatesList.size());
            int endNum = (int) (Math.random() * coordinatesList.size());
            while (startNum == endNum)
                endNum = (int) (Math.random() * coordinatesList.size());

            ObjectNode requestBodyJson = (ObjectNode) objectMapper.readTree("{}");

            requestBodyJson.put("startX", coordinatesList.get(startNum).get(0).toString());
            requestBodyJson.put("startY", coordinatesList.get(startNum).get(1).toString());
            requestBodyJson.put("endX", coordinatesList.get(endNum).get(0).toString());
            requestBodyJson.put("endY", coordinatesList.get(endNum).get(1).toString());
            requestBodyJson.put("startName", "start");
            requestBodyJson.put("endName", "end");

            String requestBody = requestBodyJson.toString();

            try (OutputStream os = conn2.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            } catch (IOException e) {
                log.error("Error writing to the connection: " + e.getMessage());
                e.printStackTrace();
            }
            jsonString = "";
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn2.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                jsonString = response.toString();
                log.info("보행자경로 response");
            } catch (IOException e) {
                log.error(e.getMessage());
            } finally {
                conn2.disconnect();
            }



            JsonNode jsonNode1 = new ObjectMapper().readTree(jsonString);
            ArrayNode arrayNode1 = (ArrayNode) jsonNode1.get("features");
            JsonNode featureJson = arrayNode1.get((int) (Math.random() * ((arrayNode.size() / 2) / 2)) * 2 + 1);
            JsonNode geometryJson2 = featureJson.get("geometry");

            JsonNode coordinatesLists = geometryJson2.get("coordinates");

            List<List<Double>> coordinatesList5 = new ArrayList<>();
            for (JsonNode coordinate : coordinatesLists) {
                if (coordinate.isArray() && coordinate.size() >= 2) {
                    double longitude = coordinate.get(0).asDouble();
                    double latitude = coordinate.get(1).asDouble();
                    coordinatesList5.add(Arrays.asList(longitude, latitude));
                }
            }
            log.info(coordinatesList5.toString());

            int num = (int) (Math.random() * coordinatesLists.size());
            BallLocation ball;

            switch (goldRan) {
                case 0: //황금원판
                    ball = new BallLocation(goldNum++, coordinatesList5.get(num).get(1), coordinatesList5.get(num).get(0));
                    log.info(String.valueOf(ball));
                    String jsonBall = new ObjectMapper().writeValueAsString(ball);
                    messagingTemplate.convertAndSend("/topic/game/random/" + gameCode, jsonBall);
                    break;
                case 1:
                    ball = new BallLocation(randNum++, coordinatesList5.get(num).get(1), coordinatesList5.get(num).get(0));
                    log.info(String.valueOf(ball));
                    jsonBall = new ObjectMapper().writeValueAsString(ball);
                    messagingTemplate.convertAndSend("/topic/game/random/" + gameCode, jsonBall);
                    break;
            }
            log.info("RandomJob!!!!!!");
        }
    }
}