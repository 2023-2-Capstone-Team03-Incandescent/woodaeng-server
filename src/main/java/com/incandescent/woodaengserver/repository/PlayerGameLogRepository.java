package com.incandescent.woodaengserver.repository;

import com.incandescent.woodaengserver.dto.PlayerMatchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;

@Repository

public class PlayerGameLogRepository {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public PlayerGameLogRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void save(PlayerMatchRequest playerMatchRequest) {
        String insertPlayerMatchQuery = "insert into player(user_id, nickname, latitude, longitude, team, game_code) values (?,?,?,?,?,?)";
        Object[] insertPlayerMatchQueryParams = new Object[]{playerMatchRequest.getId(), playerMatchRequest.getNickname(), playerMatchRequest.getLatitude(), playerMatchRequest.getLongitude(), playerMatchRequest.getTeam(), playerMatchRequest.getGame_code()};

        this.jdbcTemplate.update(insertPlayerMatchQuery, insertPlayerMatchQueryParams);
    }

    public List<PlayerMatchRequest> get() {
        String insertPlayerMatchQuery = "select * from player";

        List<PlayerMatchRequest> list = this.jdbcTemplate.query(insertPlayerMatchQuery,
            (rs, rowNum) -> new PlayerMatchRequest(
                    rs.getLong("user_id"),
                    rs.getString("nickname"),
                    rs.getDouble("latitude"),
                    rs.getDouble("longitude"),
                    rs.getInt("team"),
                    rs.getLong("game_code")));
        return list;
    }
}
