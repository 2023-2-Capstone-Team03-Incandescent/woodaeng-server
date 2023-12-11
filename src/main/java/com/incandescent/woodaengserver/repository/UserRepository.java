package com.incandescent.woodaengserver.repository;

import com.incandescent.woodaengserver.domain.Point;
import com.incandescent.woodaengserver.domain.User;
import com.incandescent.woodaengserver.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }


    public User insertUser(User user) {
        String insertUserQuery = "insert into user (email, nickname, password, role, provider, provider_id, win_cnt) values (?,?,?,?,?,?,0)";
        Object[] insertUserParams = new Object[]{user.getEmail(), user.getNickname(), user.getPassword(), user.getRole(), user.getProvider(), user.getProvider_id()};

        this.jdbcTemplate.update(insertUserQuery, insertUserParams);

        Long lastInsertId = this.jdbcTemplate.queryForObject("select last_insert_id()", Long.class);

        user.setId(lastInsertId);


        insertUserQuery = "insert into trophy (id, ball_cnt, gold_cnt, box_cnt, mini_cnt, game_cnt, win_cnt, mvp_cnt) values (?, 0, 0, 0, 0, 0, 0, 0)";
        insertUserParams = new Object[]{lastInsertId};

        this.jdbcTemplate.update(insertUserQuery, insertUserParams);

        return user;
    }

    public User selectByEmail(String email) {
        String selectByEmailQuery = "select id, email, nickname, password, role, provider, provider_id from user where email = ? and status = 1";
        Object[] selectByEmailParams = new Object[]{email};
        try {
            return this.jdbcTemplate.queryForObject(selectByEmailQuery,
                    (rs, rowNum) -> new User(
                            rs.getLong("id"),
                            rs.getString("email"),
                            rs.getString("nickname"),
                            rs.getString("password"),
                            rs.getString("role"),
                            rs.getString("provider"),
                            rs.getString("provider_id")),
                    selectByEmailParams);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public User selectById(Long user_id) {
        String selectByIdQuery = "select id, email, nickname, password, role, provider, provider_id from user where id = ? and status = 1";
        return this.jdbcTemplate.queryForObject(selectByIdQuery,
                (rs, rowNum) -> new User(
                        rs.getLong("id"),
                        rs.getString("email"),
                        rs.getString("nickname"),
                        rs.getString("password"),
                        rs.getString("role"),
                        rs.getString("provider"),
                        rs.getString("provider_id")),
                user_id);
    }

    public int checkEmail(String email) {
        String checkEmailQuery = "select exists(select email from user where email = ? and status = 1)";
        Object[] checkEmailParams = new Object[]{email};
        return this.jdbcTemplate.queryForObject(checkEmailQuery, int.class, checkEmailParams);
    }

    public int checkId(Long id) {
        String checkIdQuery = "select exists(select nickname from user where id = ? and status = 1)";
        Long checkIdParam = id;
        return this.jdbcTemplate.queryForObject(checkIdQuery, int.class, checkIdParam);
    }

    public boolean checkNickname(String nickname) {
        String checkUserQuery = "select exists(select * from user where nickname = ?)";

        int result = this.jdbcTemplate.queryForObject(checkUserQuery, int.class, nickname);

        if (result != 1)
            return false;

        return true;
    }



    public void saveProfile(UserProfileResponse userProfileResponse) {
        String updateUserQuery = "update user set nickname = ?, image_id = ?, dog_name = ?, dog_age = ?, dog_breed = ?, dog_sex = ? where id = ?";
        Object[] updateUserParams = new Object[]{userProfileResponse.getNickname(), userProfileResponse.getImage_id(), userProfileResponse.getDog_name(), userProfileResponse.getDog_age(), userProfileResponse.getDog_breed(), userProfileResponse.getDog_sex(), userProfileResponse.getId()};

        this.jdbcTemplate.update(updateUserQuery, updateUserParams);
    }

    public UserProfileResponse selectProfileById(Long id) {
        String selectUserQuery = "select id, nickname, image_id, dog_name, dog_age, dog_breed, dog_sex from user where id = ?";
        Object[] selectUserParams = new Object[]{id};

        return this.jdbcTemplate.queryForObject(selectUserQuery,
                (rs, rowNum) -> new UserProfileResponse(
                        rs.getLong("id"),
                        rs.getString("nickname"),
                        rs.getString("Image_id"),
                        rs.getString("dog_name"),
                        rs.getInt("dog_age"),
                        rs.getString("dog_breed"),
                        rs.getInt("dog_sex")
                ), selectUserParams);
    }

    public String selectImageById(Long id) {
        String selectUserQuery = "select image_id from user where id = ?";
        Object[] selectUserParams = new Object[]{id};

        return this.jdbcTemplate.queryForObject(selectUserQuery, String.class, selectUserParams);
    }


    public int getPoint(Long id) {
        String getPointQuery = "select sum(point) from point where id = ?";
        Object[] getPointParams = new Object[]{id};

        return this.jdbcTemplate.queryForObject(getPointQuery, Integer.class, getPointParams);
    }

    public TrophyInfo getTrophy(Long id) {
        String getTrophyQuery = "select * from trophy where id = ?";
        Object[] getTrophyParams = new Object[]{id};

        return this.jdbcTemplate.queryForObject(getTrophyQuery, (rs, count) -> new TrophyInfo(
                rs.getLong("id"),
                rs.getInt("ball_cnt"),
                rs.getInt("gold_cnt"),
                rs.getInt("box_cnt"),
                rs.getInt("mini_cnt"),
                rs.getInt("game_cnt"),
                rs.getInt("win_cnt"),
                rs.getInt("mvp_cnt")
        ), getTrophyParams);
    }

    public List<GameRecordInfo> getGameRecord(Long id) {
        String getGameRecordQuery = "select * from gameRecord where id = ? order by time desc";
        Object[] getGameRecordParams = new Object[]{id};

        return this.jdbcTemplate.query(getGameRecordQuery, ((rs, count) -> new GameRecordInfo(
                rs.getLong("id"),
                rs.getString("game_code"),
                rs.getInt("mvp"),
                rs.getInt("win"),
                rs.getString("location"),
                rs.getInt("ball_cnt"),
                rs.getTimestamp("time"))
        ), getGameRecordParams);
    }

    public List<Point> getPointList(Long id) {
        String getPointListQuery = "select * from point where id = ?";
        Object[] getPointListParams = new Object[]{id};

        return this.jdbcTemplate.query(getPointListQuery, ((rs, rowNum) -> new Point(
                rs.getLong("id"),
                rs.getInt("point"),
                rs.getString("detail"),
                rs.getTimestamp("time"))
        ), getPointListParams);
    }

    public List<Ranking> getRankingList() {
        String getRankingQuery = "select id, rank() over (order by win_cnt desc) AS rank, image_id, nickname, win_cnt from user order by win_cnt desc";

        return this.jdbcTemplate.query(getRankingQuery, ((rs, rowNum) -> new Ranking(
                rs.getLong("id"),
                rs.getInt("rank"),
                rs.getString("image_id"),
                rs.getString("nickname"),
                rs.getInt("win_cnt"))
        ));
    }
    public int getMyRank(Long id) {
        String selectUserQuery = "select rank() over (order by win_cnt desc) from user where id = ?";
        Object[] selectUserParams = new Object[]{id};

        return this.jdbcTemplate.queryForObject(selectUserQuery, Integer.class, selectUserParams);
    }
}