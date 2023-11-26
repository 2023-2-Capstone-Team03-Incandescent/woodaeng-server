package com.incandescent.woodaengserver.repository;

import com.incandescent.woodaengserver.domain.Player;
import com.incandescent.woodaengserver.dto.game.BallLocation;
import com.incandescent.woodaengserver.dto.game.GamePlayerResult;
import com.incandescent.woodaengserver.dto.game.GameResultResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.ArrayList;
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
        String insertGameQuery = "insert into player (user_id, latitude, longitude, team, game_code, ball_cnt, gold_cnt, box_cnt, mini_cnt) values (?,?,?,0,\"0\",0,0,0,0)";
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

//    public List<Object> selectDog(Long id) {
//        //이름 나이 견종 성별
//        String selectDogQuery = "select dog_name, dog_age, dog_breed, dog_sex from user where user_id = ?";
//        Object[] selectDogParams = new Object[]{id};
//
//        List<Object> returnList = this.jdbcTemplate.query(selectDogQuery, new Object[], selectDogParams);
//
//
////        List<Object> returnList = new ArrayList<>();
////        returnList.add();
////        returnList.add();
////        returnList.add();
////        returnList.add();
//        return
//    }
}