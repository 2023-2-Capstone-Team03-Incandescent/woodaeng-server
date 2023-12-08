package com.incandescent.woodaengserver.repository;

import com.incandescent.woodaengserver.dto.DogInfo;
import com.incandescent.woodaengserver.dto.game.BallLocation;
import com.incandescent.woodaengserver.dto.game.GamePlayerResult;
import com.incandescent.woodaengserver.dto.game.GameResultResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Repository
public class GameRepository {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public GameRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }



    public void insertPlayer(Long id, double latitude, double longitude) {
        String insertGameQuery = "insert into player (user_id, latitude, longitude, team, game_code, ball_cnt, gold_cnt, box_cnt, mini_cnt) values (?,?,?,0,'0',0,0,0,0)";
        Object[] insertGameParams = new Object[]{id, latitude, longitude};

        this.jdbcTemplate.update(insertGameQuery, insertGameParams);
    }

    public void updatePlayer(String game_code, List<Long> teamRed, List<Long> teamBlue) {
        for (Long id : teamRed) {
            String updatePlayerQuery = "update player set game_code = ?, team = 0 where user_id = ?";
            Object[] updatePlayerParams = new Object[]{game_code, id};

            this.jdbcTemplate.update(updatePlayerQuery, updatePlayerParams);
        }
        for (Long id : teamBlue) {
            String updatePlayerQuery = "update player set game_code = ?, team = 1 where user_id = ?";
            Object[] updatePlayerParams = new Object[]{game_code, id};

            this.jdbcTemplate.update(updatePlayerQuery, updatePlayerParams);
        }

    }
    
    public void updatePlayerLocation(Long id, double latitude, double longitude) {
        String updatePlayerLocationQuery = "update player set latitude = ?, longitude = ? where user_id = ?";
        Object[] updatePlayerLocationParams = new Object[]{latitude, longitude, id};
        
        this.jdbcTemplate.update(updatePlayerLocationQuery, updatePlayerLocationParams);
    }
    
    public Long selectNearPlayer(Long id) {
        HashMap<String, Double> location = this.jdbcTemplate.queryForObject(
                "select latitude, longitude from player where user_id = ?",
                ((rs, rowNum) -> new HashMap<String, Double>() {{
                        put("latitude", rs.getDouble("latitude"));
                        put("longitude", rs.getDouble("longitude"));
                }}),
                id);

        List<HashMap<String, Object>> sqlList = this.jdbcTemplate.query(
                "select user_id, latitude, longitude from player where user_id != ?",
                (rs, rowNum) -> {
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("id", rs.getLong("user_id"));
                    map.put("latitude", rs.getDouble("latitude"));
                    map.put("longitude", rs.getDouble("longitude"));
                    return map;
                },
                id);

        Long nearPlayer = null;
        
        for (int i = 0; i < 5; i++) {
            double lat = (double) sqlList.get(i).get("latitude");
            double lon = (double) sqlList.get(i).get("longitude");

            if ((location.get("latitude") - lat) * 60 * 60 * 30 * (location.get("latitude") - lat) * 60 * 60 * 30 + (location.get("longitude") - lon) * 60 * 60 * 24 * (location.get("longitude") - lon) * 60 * 60 * 24 < 100) {
                nearPlayer = (Long) sqlList.get(i).get("id");
            }

            if (nearPlayer != null) {
                break;
            }
        }
        
        return nearPlayer;
    }

    public void insertBalls (String game_code, List<BallLocation> balls) {
        String insertGameQuery = "insert into game (game_code, red_score) values (?,10)";
        Object[] insertGameParams = new Object[]{game_code};

        this.jdbcTemplate.update(insertGameQuery, insertGameParams);

        for (int i = 0; i < 20; i++) {
            insertGameQuery = "insert into ball (ballId, game_code, latitude, longitude, color) values (?,?,?,?,?)";
            insertGameParams = new Object[]{i, game_code, balls.get(i).getLatitude(), balls.get(i).getLongitude(), i < 10 ? 0 : 1};

            this.jdbcTemplate.update(insertGameQuery, insertGameParams);
        }
    }

    public void updateBallColor(String game_code, Long playerId, int ballId, int color, int red_score) {
        String updateBallColorQuery = "update ball set color = ? where game_code = ? and ballId = ?";
        Object[] updateBallColorParams = new Object[]{color,game_code,ballId};

        this.jdbcTemplate.update(updateBallColorQuery, updateBallColorParams);


        updateBallColorQuery = "update game set red_score = ? where game_code = ?";
        updateBallColorParams = new Object[]{red_score,game_code};

        this.jdbcTemplate.update(updateBallColorQuery, updateBallColorParams);


        if(selectTeam(playerId) == color)
            updateBallColorQuery = "update player set ball_cnt = ball_cnt + 1 where user_id = ?";
        else
            updateBallColorQuery = "update player set ball_cnt = ball_cnt - 1 where user_id = ?";

        updateBallColorParams = new Object[]{playerId};

        this.jdbcTemplate.update(updateBallColorQuery, updateBallColorParams);
    }

    public void updateGoldNum(Long playerId) {
        String updateGoldNumQuery = "update player set gold_cnt = gold_cnt + 1 where user_id = ?";
        Object[] updateGoldNumParams = new Object[]{playerId};

        this.jdbcTemplate.update(updateGoldNumQuery, updateGoldNumParams);
    }

    public void updateRBNum(Long playerId) {
        String updateRBNumQuery = "update player set box_cnt = box_cnt + 1 where user_id = ?";
        Object[] updateRBNumParams = new Object[]{playerId};

        this.jdbcTemplate.update(updateRBNumQuery, updateRBNumParams);
    }

    public Integer selectTeam(Long playerId) {
        String selectTeamQuery = "select team from player where user_id = ?";
        Object[] selectTeamParams = new Object[]{playerId};

        return this.jdbcTemplate.queryForObject(selectTeamQuery, Integer.class, selectTeamParams);
    }

    public Integer selectRedScore(String game_code) {
        String selectRedScoreQuery = "select red_score from game where game_code = ?";
        Object[] selectRedScoreParams = new Object[]{game_code};

        return this.jdbcTemplate.queryForObject(selectRedScoreQuery, Integer.class, selectRedScoreParams);
    }

    public Integer selectBallColor(String game_code, int ballId) {
        String selectBallColorQuery = "select color from ball where game_code = ? and ballId = ?";
        Object[] selectBallColorParams = new Object[]{game_code, ballId};

        return this.jdbcTemplate.queryForObject(selectBallColorQuery, Integer.class, selectBallColorParams);
    }

    public List<Integer> selectOurBall(String game_code, int color) {
        String sselectOurBallQuery = "select ballId from ball where game_code = ? and color = ?";
        Object[] selectOurBallParams = new Object[]{game_code, color};

        List<Integer> sqlList = this.jdbcTemplate.query(sselectOurBallQuery, ((rs, rowNum) -> rs.getInt("ballId")), selectOurBallParams);
        return sqlList;
    }

    public GameResultResponse selectResult(String game_code) {
        String selectResultQuery = "select red_score from game where game_code = ?";
        Object[] selectResultParams = new Object[]{game_code};

        int redS = this.jdbcTemplate.queryForObject(selectResultQuery, Integer.class, selectResultParams);
        int team;
        if (redS > 10)
            team = 0;
        else if (redS < 10)
            team = 1;
        else
            team = 2;
        int blueS = 20 - redS;

        selectResultQuery = "select user_id, team, ball_cnt, gold_cnt, box_cnt, mini_cnt from player where game_code = ?";
        selectResultParams = new Object[]{game_code};

        List<GamePlayerResult> sqlList = this.jdbcTemplate.query(selectResultQuery, (rs, count) -> new GamePlayerResult(
                rs.getLong("user_id"),
                rs.getInt("team"),
                rs.getInt("ball_cnt"),
                rs.getInt("gold_cnt"),
                rs.getInt("box_cnt"),
                rs.getInt("mini_cnt")
        ), selectResultParams);

        return new GameResultResponse(team, redS, blueS, sqlList);
    }

    public DogInfo selectDog(Long id) {
        //이름 나이 견종 성별
        String selectDogQuery = "select dog_name, dog_age, dog_breed, dog_sex from user where id = ?";
        Object[] selectDogParams = new Object[]{id};
        return this.jdbcTemplate.queryForObject(selectDogQuery, (rs, count) -> new DogInfo(
                rs.getString("dog_name"),
                rs.getInt("dog_age"),
                rs.getString("dog_breed"),
                rs.getInt("dog_sex")), selectDogParams);
    }

    public void saveRecord(String gameCode, GameResultResponse gameResultResponse, int mvp, String dongName) {
        String saveRecordQuery = "insert into gameRecord (id, game_code, mvp, win, location, ball_cnt) values (?,?,?,?,?,?)";
        Object[] saveRecordParams;
        for (int i = 0; i < gameResultResponse.getPlayerResults().size(); i++) {
            GamePlayerResult playerResult = gameResultResponse.getPlayerResults().get(i);
            saveRecordParams = new Object[]{playerResult.getId(), gameCode, mvp, gameResultResponse.getTeam() == playerResult.getTeam() ? 1 : 0, dongName, playerResult.getBall_cnt()};
            this.jdbcTemplate.update(saveRecordQuery, saveRecordParams);

            if (gameResultResponse.getTeam() == playerResult.getTeam())
                addPoint(playerResult.getId(), 30, "게임 승리");

            if (mvp == playerResult.getId())
                addPoint(playerResult.getId(), 100, "MVP 달성");


            String savetrophyQuery = "select * from trohpy where id = ?";
            saveRecordParams = new Object[]{playerResult.getId()};
            HashMap trophyMap = this.jdbcTemplate.queryForObject(savetrophyQuery, (rs, rowNum) -> new HashMap() {{
                put("ball_cnt", rs.getInt("ball_cnt"));
                put("gold_cnt" ,rs.getInt("gold_cnt"));
                put("box_cnt", rs.getInt("box_cnt"));
                put("mini_cnt" ,rs.getInt("mini_cnt"));
                put("game_cnt", rs.getInt("game_cnt"));
                put("win_cnt" ,rs.getInt("win_cnt"));
                put("mvp_cnt", rs.getInt("mvp_cnt"));
            }}, saveRecordParams);
            List trophyList = new ArrayList();
            if ((int) trophyMap.get("ball_cnt") < 50)
                trophyList.add("ball_cnt");
            if ((int) trophyMap.get("gold_cnt") < 15)
                trophyList.add("gold_cnt");
            if ((int) trophyMap.get("box_cnt") < 15)
                trophyList.add("box_cnt");
            if ((int) trophyMap.get("mini_cnt") < 10)
                trophyList.add("mini_cnt");
            if ((int) trophyMap.get("game_cnt") < 20)
                trophyList.add("game_cnt");
            if ((int) trophyMap.get("win_cnt") < 10)
                trophyList.add("win_cnt");
            if ((int) trophyMap.get("mvp_cnt") < 5)
                trophyList.add("mvp_cnt");


            savetrophyQuery = "update trophy set ball_cnt = ball_cnt + ?, gold_cnt = gold_cnt + ?, box_cnt = box_cnt + ?, mini_cnt = mini_cnt + ?, game_cnt = game_cnt + 1, win_cnt = win_cnt + ?, mvp_cnt = mvp_cnt + ? where id = ?";
            saveRecordParams = new Object[]{playerResult.getBall_cnt(), playerResult.getGold_cnt(), playerResult.getBox_cnt(), playerResult.getMini_cnt(), gameResultResponse.getTeam() == playerResult.getTeam() ? 1 : 0, mvp == playerResult.getId() ? 1 : 0, playerResult.getId()};
            this.jdbcTemplate.update(savetrophyQuery, saveRecordParams);


            savetrophyQuery = "select * from trohpy where id = ?";
            saveRecordParams = new Object[]{playerResult.getId()};
            HashMap updatedMap = this.jdbcTemplate.queryForObject(savetrophyQuery, (rs, rowNum) -> new HashMap() {{
                for (Object str : trophyList) {
                    put(str.toString(), rs.getInt(str.toString()));
                }
            }}, saveRecordParams);

            if ((int) updatedMap.getOrDefault("ball_cnt", 0) >= 50)
                addPoint(playerResult.getId(), 30, "업적 달성");
            if ((int) updatedMap.getOrDefault("gold_cnt", 0) >= 15)
                addPoint(playerResult.getId(), 30, "업적 달성");
            if ((int) updatedMap.getOrDefault("box_cnt", 0) >= 15)
                addPoint(playerResult.getId(), 30, "업적 달성");
            if ((int) updatedMap.getOrDefault("mini_cnt", 0) >= 10)
                addPoint(playerResult.getId(), 40, "업적 달성");
            if ((int) updatedMap.getOrDefault("game_cnt", 0) >= 20)
                addPoint(playerResult.getId(), 40, "업적 달성");
            if ((int) updatedMap.getOrDefault("win_cnt", 0) >= 10)
                addPoint(playerResult.getId(), 50, "업적 달성");
            if ((int) updatedMap.getOrDefault("mvp_cnt", 0) >= 5)
                addPoint(playerResult.getId(), 50, "업적 달성");

        }
    }

    public void addPoint(Long id, int point, String detail) {
        String addPointQuery = "insert into point (id, point, detail) values (?,?,?)";
        Object[] addPointParams = new Object[]{point, id, detail};

        this.jdbcTemplate.update(addPointQuery, addPointParams);
    }
}