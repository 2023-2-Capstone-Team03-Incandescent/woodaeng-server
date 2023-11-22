package com.incandescent.woodaengserver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.incandescent.woodaengserver.domain.Player;
import com.incandescent.woodaengserver.dto.game.BallLocation;
import com.incandescent.woodaengserver.dto.game.PlayerMatchResponse;
import com.incandescent.woodaengserver.repository.GameRepository;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.io.OutputStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;


@Slf4j
@Service
public class GameMatchingService {
    @Value("${api.appKey}")
    private String appKey;

    private final RedisTemplate<String, String> redisTemplate;
    private final SetOperations<String, String> setOperations;
    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, AtomicLong> cellIdCounterMap = new ConcurrentHashMap<>();
    private final GameRepository gameRepository;
    private List<Player> players;

    @Autowired
    public GameMatchingService(RedisTemplate<String, String> redisTemplate, SimpMessagingTemplate messagingTemplate, GameRepository gameRepository) {
        this.redisTemplate = redisTemplate;
        this.setOperations = redisTemplate.opsForSet();
        this.messagingTemplate = messagingTemplate;
        this.gameRepository = gameRepository;
    }

    public synchronized void joinLocationQueue(String playerId, double latitude, double longitude) throws IOException {
        log.info("joinLocationQueue");

        String gameCode;

        URL locationUrl = new URL("https://apis.openapi.sk.com/tmap/geofencing/regions?version=1&count=20&categories=adminDong&searchType=COORDINATES&reqCoordType=WGS84GEO&reqLon="+longitude+"&reqLat="+latitude);
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
            log.info(e.getMessage());
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            JsonNode arrayNode = jsonNode.get("searchRegionsInfo");
            JsonNode searchRegionsInfoJson = arrayNode.get(0);
            JsonNode regionInfoJson = searchRegionsInfoJson.get("regionInfo");
            String dongId = String.valueOf(regionInfoJson.get("regionId").asInt());

         setOperations.add("locationQueue:" + dongId, playerId);
            notifyAll();
            gameCode = tryMatch(dongId);
        } catch (ParseException e) {
            log.info(String.valueOf(e.getErrorType()));
        }


//        players.add(new Player(playerId, latitude, longitude, team, gameCode));
    }

    private synchronized String tryMatch(String dongId) throws IOException, ParseException {
        while (true) {
            log.info("tryMatch");
            Set<String> locationQueue = setOperations.members("locationQueue:" + dongId);
            log.info("QUEUE SIZE: "+locationQueue.size());

            if (locationQueue.size() >= 2) {
                AtomicLong counter = cellIdCounterMap.computeIfAbsent(String.valueOf(dongId), k -> new AtomicLong());
                long incrementValue = counter.getAndIncrement();

                String formattedIncrementValue = String.format("%02d", incrementValue);
                String gameCode = dongId + formattedIncrementValue;



                sendMatchInfo(locationQueue, gameCode);
                setOperations.remove("locationQueue:" + dongId, locationQueue.toArray());
                cellIdCounterMap.remove(dongId);
                return gameCode;
            }

            try {
                log.info("wait");
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while waiting for match");
                return "INTERRRR";  // 대기 중 인터럽트 발생 시 메서드 종료
            }
        }
    }

    private void sendMatchInfo(Set<String> playerIds, String gameCode) throws IOException {
        log.info("sendMatchInfo");
        List<String> playerList = new ArrayList<>(playerIds);
        Collections.shuffle(playerList);

        List<String> teamRed = playerList.subList(0, 3);
        List<String> teamBlue = playerList.subList(3, 6);

        List<BallLocation> balls = new ArrayList<>();




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




        while(balls.size() < 20) {
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
                e.printStackTrace();  // Log the full stack trace for debugging.
            }
            jsonString = "";
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn2.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                jsonString = response.toString();
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


            if((featureJson.get("properties")).get("facilityType").equals("11")) {
                int i = (int) (Math.random() * coordinatesLists.size());
                balls.add(new BallLocation(coordinatesList5.get(i).get(1), coordinatesList5.get(i).get(0)));
            }
        }

        PlayerMatchResponse playerMatchResponse = new PlayerMatchResponse(gameCode, teamRed, teamBlue, balls);



//        gameRepository.insertGame(gameCode, balls, players);

        // 각 팀에게 팀 정보 및 게임 코드 전송
        messagingTemplate.convertAndSend("/game/matching", playerMatchResponse);
        log.info("matching");
        log.info(playerMatchResponse.toString());
    }
}
