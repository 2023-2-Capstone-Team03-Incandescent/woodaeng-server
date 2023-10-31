package com.incandescent.woodaengserver.service;

import com.incandescent.woodaengserver.domain.User;
import com.incandescent.woodaengserver.repository.UserRepository;
import com.incandescent.woodaengserver.util.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.incandescent.woodaengserver.util.ResponseStatus.*;

@Slf4j
@Service
public class UserProvider {

    private final UserRepository userRepository;

    @Autowired
    public UserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User retrieveByEmail(String email) throws CustomException {
        if (checkEmail(email) == 0)
            throw new CustomException(USERS_EMPTY_USER_EMAIL);
        try {
            return userRepository.selectByEmail(email);
        } catch (Exception e) {
            throw new CustomException(DATABASE_ERROR);
        }

    }

    public User retrieveById(Long user_id) throws CustomException {
        if (checkId(user_id) == 0)
            throw new CustomException(USERS_EMPTY_USER_ID);
        try {
            return userRepository.selectById(user_id);
        } catch (Exception e) {
            throw new CustomException(DATABASE_ERROR);
        }
    }

    public int checkEmail(String email) throws CustomException {
        try {
            return userRepository.checkEmail(email);
        } catch (Exception exception) {
            log.warn(exception.getMessage());
            throw new CustomException(DATABASE_ERROR);
        }
    }


    public int checkId(Long id) throws CustomException {
        try {
            return userRepository.checkId(id);
        } catch (Exception exception) {
            log.warn(exception.getMessage());
            throw new CustomException(DATABASE_ERROR);
        }
    }
}