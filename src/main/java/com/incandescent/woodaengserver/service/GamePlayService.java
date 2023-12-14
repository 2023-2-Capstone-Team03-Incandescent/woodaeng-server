package com.incandescent.woodaengserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.incandescent.woodaengserver.dto.DogInfo;
import com.incandescent.woodaengserver.dto.UserProfileResponse;
import com.incandescent.woodaengserver.dto.game.*;
import com.incandescent.woodaengserver.repository.GameRepository;
import com.incandescent.woodaengserver.repository.UserRepository;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.domain.geo.Metrics;
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
import java.util.*;

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
    private static UserRepository userRepository;
    private static String dongName = null;

    @Autowired
    public GamePlayService(RedisMessageListenerContainer container, RedisTemplate<String, String> redisTemplate, SimpMessagingTemplate messagingTemplate, RedisPublisher redisPublisher, RedisSubscriber redisSubscriber, GameRepository gameRepository, UserRepository userRepository) {
        this.redisTemplate = redisTemplate;
        this.container = container;
        this.messagingTemplate = messagingTemplate;
        this.redisPublisher = redisPublisher;
        this.redisSubscriber = redisSubscriber;
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
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
        LocalDateTime endTime = startTime.plusMinutes(15); // 15분

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
            int randomTime = (int) (Math.random() * 3) + 3; // 3 ~ 5분
            tempTime.plusMinutes(randomTime);
//            int randomTime = (int) (Math.random() * 30) + 10; //10 ~ 40초
//            tempTime = tempTime.plusSeconds(randomTime);

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
                case 2: //안개
                    gamePlayResponse = new GamePlayResponse(gamePlayRequest.getId(), gamePlayRequest.getTeam(), 40, 40, redScore, 20-redScore, random);
                    break;
                case 3: //blue+
                    List<Integer> redBalls = gameRepository.selectOurBall(gameCode, 0);
                    int ball = redBalls.get((int) (Math.random() * redBalls.size()));
                    gameRepository.updateBallColor(gameCode, gamePlayRequest.getId(), ball, 1, --redScore);

                    gamePlayResponse = new GamePlayResponse(gamePlayRequest.getId(), gamePlayRequest.getTeam(), ball, 40, redScore, 20-redScore, random);
                    break;
                case 4: // 포인트
                    gamePlayResponse = new GamePlayResponse(gamePlayRequest.getId(), gamePlayRequest.getTeam(), 40, 40, redScore, 20-redScore, random);
                    gameRepository.addPoint(gamePlayRequest.getId(), 50, "랜덤 박스");
                    break;
                case 5: //red+
                    List<Integer> blueBalls = gameRepository.selectOurBall(gameCode, 1);
                    ball = blueBalls.get((int) (Math.random() * blueBalls.size()));
                    gameRepository.updateBallColor(gameCode, gamePlayRequest.getId(), ball, 0, ++redScore);

                    gamePlayResponse = new GamePlayResponse(gamePlayRequest.getId(), gamePlayRequest.getTeam(), ball, 40, redScore, 20-redScore, random);
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

    public List<PlayerLocation> findNearByLocation(Long playerId, double latitude, double longitude, int team) {
        GeoOperations<String, String> ops = redisTemplate.opsForGeo();
        GeoResults<RedisGeoCommands.GeoLocation<String>> result = ops.radius(
                "NearPlayer",
                new Circle(new Point(longitude, latitude), new Distance(3, Metrics.METERS)),
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeDistance());
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = result.getContent();

        List<PlayerLocation> rs = new ArrayList<>();
        list.stream()
                .filter(obj -> {
                    Long id = Long.valueOf(obj.getContent().getName());
                    int playerTeam = gameRepository.selectTeam(id);
                    return playerTeam != team && id != playerId;
                })
                .forEach(obj->{
                    Long id = Long.valueOf(obj.getContent().getName());
                    int playerTeam = gameRepository.selectTeam(id);
                    double retLatitude = obj.getContent().getPoint().getY();
                    double retLongitude = obj.getContent().getPoint().getX();
                    rs.add(new PlayerLocation(id, playerTeam, new Point(retLongitude, retLatitude)));
                });

        return rs;
    }


    public void realTimeLocation(String gameCode, PlayerMatchRequest playerMatchRequest) throws JsonProcessingException {
//        GeoOperations<String, String> ops = redisTemplate.opsForGeo();
//        ops.remove("NearPlayer", String.valueOf(playerMatchRequest.getId()));
//        ops.add("NearPlayer", new Point(playerMatchRequest.getLongitude(), playerMatchRequest.getLatitude()), String.valueOf(playerMatchRequest.getId()));
//
//        List<PlayerLocation> nearPlayerList = findNearByLocation(playerMatchRequest.getId(), playerMatchRequest.getLatitude(), playerMatchRequest.getLongitude(), gameRepository.selectTeam(playerMatchRequest.getId()));
//
//        if (nearPlayerList.isEmpty())
//            return;
//        Long opponentId = nearPlayerList.get(0).getId();

        int tf = gameRepository.updatePlayerLocation(playerMatchRequest.getId(), playerMatchRequest.getLatitude(), playerMatchRequest.getLongitude());

        if (tf == 0)
            return;

        Long opponentId = gameRepository.selectNearPlayer(playerMatchRequest.getId());
        if (opponentId == null)
            return;

        int gameType = Math.round((float)Math.random());
        String question = null;
        HashMap<Integer, String> options = null;
        int answer = 0;

        switch (gameType) {
            case 0: //댕댕퀴즈
                List<Object> quiz = getQuiz(opponentId);
                question = (String) quiz.get(0);
                options = (HashMap<Integer, String>) quiz.get(1);
                answer = (int) quiz.get(2);
                break;
            case 1: //댕기자랑
                String[] commands = {"앉아", "엎드려", "손"};
                question = commands[(int) (Math.random() * commands.length)];
                break;
        }

        LocalDateTime startTime = LocalDateTime.now().plusSeconds(5);

        Long id = playerMatchRequest.getId();
        UserProfileResponse profile = userRepository.selectProfileById(id);
        UserProfileResponse opProf = userRepository.selectProfileById(opponentId);
        GameMiniResponse gameMiniResponse = new GameMiniResponse(startTime.toString(), id, profile.getImage_id(), profile.getDog_name(), gameRepository.selectTeam(id), gameType, opponentId, opProf.getImage_id(), opProf.getDog_name(), question, options, answer);
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonGameLocationResponse = objectMapper.writeValueAsString(gameMiniResponse);
        messagingTemplate.convertAndSend("/topic/game/location/" + gameCode, jsonGameLocationResponse);
        log.info(jsonGameLocationResponse);
    }

    public List<Object> getQuiz(Long id) {

        List<Object> quiz = new ArrayList<>();
        int questionType = (int) (Math.random() * 3);

        String question = null;
        HashMap<Integer, String> options = new HashMap<>();
        int answer = 0;

//        DogInfo dog = gameRepository.selectDog(id);

        DogInfo dog = gameRepository.selectDog(1L);

        switch (questionType) {
            case 0: //나이
                question = dog.getName() + "의 나이는 몇 살일까요?";
                int age = dog.getAge();
                answer = (int) (Math.random() * 4) + 1;

                options.put(answer, age+"살"); //정답

                int value = age;
                int num = answer;
                while (num > 0) {
                    options.put(--num, --value+"살");
                }
                num = answer;
                value = age;
                while (num < 5) {
                    options.put(++num, ++value+"살");
                }
                break;
            case 1: //견종
                question = dog.getName() + "는 무슨 종일까요?";
                String breed = dog.getBreed();
                answer = (int) (Math.random() * 4) + 1;

                List<String> breedList = new ArrayList<>();
                breedList.add("말티스");
                breedList.add("푸들");
                breedList.add("스피츠");
                breedList.add("치와와");
                breedList.add("포메라니안");
                breedList.add("토이푸들");
                breedList.add("시츄");
                breedList.add("비숑 프리제");
                breedList.add("웰시코기");
                breedList.add("사모예드");

                options.put(answer, breed); //정답

                List<Integer> ops = new ArrayList<>();
                ops.add(breedList.indexOf(breed));
                while (ops.size() < 5) {
                    int index = (int) (Math.random() * breedList.size());
                    if (!ops.contains(index))
                        ops.add(index);
                }
                ops.remove(breedList.indexOf(breed));
                num = answer;
                int i = 0;
                while (num > 0) {
                    options.put(--num, breedList.get(i++));
                }
                num = answer;
                while (num < 5) {
                    options.put(++num, breedList.get(i++));
                }
                break;
//            case 2: //성별
//                question = dog.getName() + "의 성별은 무엇일까요?";
//                int sex = dog.getSex();
//                options.put(1, "남자");
//                options.put(2, "여자");
//                answer = sex + 1;
//                break;
            case 2: //상식
                question = "다음 중 댕댕이가 절대 먹으면 안 되는 음식은?";
                options.put(0, "닭가슴살");
                options.put(1, "포도");
                options.put(2, "고구마");
                options.put(3, "브로콜리");
                answer = 1;
                break;
        }


        quiz.add(question);
        quiz.add(options);
        quiz.add(answer);

        return quiz;
    }

    public void miniResult(String gameCode, PlayerMiniWinner playerMiniWinner) {
        Long winnerId;

        if (playerMiniWinner.getWin() == 0)
            return;

        winnerId = playerMiniWinner.getId();


        int winnerTeam = gameRepository.selectTeam(winnerId);
        int ball1, ball2;

        if (winnerTeam == 0) {
            List<Integer> otherBalls = gameRepository.selectOurBall(gameCode, 1);
            ball1 = otherBalls.get((int) (Math.random() * otherBalls.size()));
            gameRepository.updateBallColor(gameCode, winnerId, ball1, gameRepository.selectTeam(winnerId), gameRepository.selectRedScore(gameCode) + 1);

            ball2 = otherBalls.get((int) (Math.random() * otherBalls.size()));
            while (true) {
                if (ball1 != ball2)
                    break;
                ball2 = otherBalls.get((int) (Math.random() * otherBalls.size()));
            }
            gameRepository.updateBallColor(gameCode, winnerId, ball2, gameRepository.selectTeam(winnerId), gameRepository.selectRedScore(gameCode) + 1);
        } else {
            List<Integer> otherBalls = gameRepository.selectOurBall(gameCode, 0);
            ball1 = otherBalls.get((int) (Math.random() * otherBalls.size()));
            gameRepository.updateBallColor(gameCode, winnerId, ball1, gameRepository.selectTeam(winnerId), gameRepository.selectRedScore(gameCode) - 1);

            ball2 = otherBalls.get((int) (Math.random() * otherBalls.size()));
            while (true) {
                if (ball1 != ball2)
                    break;
                ball2 = otherBalls.get((int) (Math.random() * otherBalls.size()));
            }
            gameRepository.updateBallColor(gameCode, winnerId, ball2, gameRepository.selectTeam(winnerId), gameRepository.selectRedScore(gameCode) - 1);
        }

        int rs = gameRepository.selectRedScore(gameCode);

        MiniWinnerResponse miniWinnerResponse = new MiniWinnerResponse(winnerId, userRepository.selectProfileById(winnerId).getDog_name(), ball1, ball2, rs, 20-rs);

        messagingTemplate.convertAndSend("/topic/game/mini/" + gameCode, miniWinnerResponse);
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

            List<Integer> ball_cnt = new ArrayList<>();
            for (int i = 0; i < gameResultResponse.getPlayerResults().size(); i++) {
                ball_cnt.add(gameResultResponse.getPlayerResults().get(i).getBall_cnt());
            }


            gameRepository.saveRecord(gameCode, gameResultResponse, ball_cnt.indexOf(Collections.max(ball_cnt)), dongName);
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

            dongName = featuresJson.get("properties").get("regionName").toString();
            dongName = dongName.substring(1,dongName.length()-1);

            List<List<Double>> coorRes = new ArrayList<>();
            for (JsonNode coordinate : coordinatesList) {
                if (coordinate.isArray() && coordinate.size() >= 2) {
                    double longitude = coordinate.get(1).asDouble();
                    double latitude = coordinate.get(0).asDouble();
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
                    double longitude = coordinate.get(1).asDouble();
                    double latitude = coordinate.get(0).asDouble();
                    coordinatesList5.add(Arrays.asList(longitude, latitude));
                }
            }
            log.info(coordinatesList5.toString());

            int num = (int) (Math.random() * coordinatesLists.size());
            BallLocation ball;

            switch (goldRan) {
                case 0: //황금원판
                    ball = new BallLocation(goldNum++, coordinatesList5.get(num).get(0), coordinatesList5.get(num).get(1));
                    log.info(String.valueOf(ball));
                    String jsonBall = new ObjectMapper().writeValueAsString(ball);
                    messagingTemplate.convertAndSend("/topic/game/random/" + gameCode, jsonBall);
                    break;
                case 1: //랜덤박스
                    ball = new BallLocation(randNum++, coordinatesList5.get(num).get(0), coordinatesList5.get(num).get(1));
                    log.info(String.valueOf(ball));
                    jsonBall = new ObjectMapper().writeValueAsString(ball);
                    messagingTemplate.convertAndSend("/topic/game/random/" + gameCode, jsonBall);
                    break;
            }
            log.info("RandomJob!!!!!!");
        }
    }
}