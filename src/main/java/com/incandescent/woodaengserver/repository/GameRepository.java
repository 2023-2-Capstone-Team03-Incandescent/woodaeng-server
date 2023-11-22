package com.incandescent.woodaengserver.repository;

import com.incandescent.woodaengserver.domain.Player;
import com.incandescent.woodaengserver.dto.game.BallLocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;

@Repository
public class GameRepository {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public GameRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }



    public void insertGame(String game_code, List<BallLocation> balls, List<Player> players) {

        String insertGameQuery = "insert into game (game_code, red_score) values (?,10)";
        Object[] insertGameParams = new Object[]{game_code};

        this.jdbcTemplate.update(insertGameQuery, insertGameParams);

        for (int i = 0; i < 20; i++) {
            insertGameQuery = "insert into ball (ballId, game_code, latitude, longitude, color) values (?,?,?,?,?)";
            insertGameParams = new Object[]{i, game_code, balls.get(i).getLatitude(), balls.get(i).getLongitude(), i < 10 ? 0 : 1};

            this.jdbcTemplate.update(insertGameQuery, insertGameParams);
        }

        for (int i = 0; i < 6; i++) {
            insertGameQuery = "insert into player (user_id, latitude, longitude, team, game_code, ball_cnt, gold_cnt, box_cnt, mini_cnt) values (?,?,?,?,?,0,0,0,0)";
            insertGameParams = new Object[]{players.get(i).getUser_id(), players.get(i).getLatitude(), players.get(i).getLongitude(), players.get(i).getTeam(), game_code};

            this.jdbcTemplate.update(insertGameQuery, insertGameParams);
        }

    }

    public void updateBallColor(String game_code, int playerId, int ballId, int color, int red_score) {
        String updateBallColorQuery = "update ball set color = ? where game_code = ? and ballId = ?";
        Object[] updateBallColorParams = new Object[]{color,game_code,ballId};

        this.jdbcTemplate.update(updateBallColorQuery, updateBallColorParams);



        updateBallColorQuery = "update game set red_score = ? where game_code = ?";
        updateBallColorParams = new Object[]{red_score,game_code};

        this.jdbcTemplate.update(updateBallColorQuery, updateBallColorParams);


        updateBallColorQuery = "update player set ball_cnt = ball_cnt + 1 where user_id = ?";
        updateBallColorParams = new Object[]{playerId};

        this.jdbcTemplate.update(updateBallColorQuery, updateBallColorParams);
    }

    public int selectRedScore(String game_code) {
        String selectRedScoreQuery = "select red_score where game_code = ?";
        Object[] selectRedScoreParams = new Object[]{game_code};

        this.jdbcTemplate.update(selectRedScoreQuery, selectRedScoreParams);
        return 0;
    }
}