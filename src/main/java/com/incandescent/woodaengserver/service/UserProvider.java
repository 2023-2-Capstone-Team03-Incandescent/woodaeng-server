package com.incandescent.woodaengserver.service;

import com.incandescent.woodaengserver.domain.User;
import com.incandescent.woodaengserver.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserProvider {

    private final UserRepository userRepository;

    @Autowired
    public UserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User retrieveByEmail(String email) throws Exception {
        if (checkEmail(email) == 0)
            throw new Exception("Invalid Email");
        try {
            return userRepository.selectByEmail(email);
        } catch (Exception e) {
            throw new Exception("Database Error");
        }

    }

    public User retrieveById(Long user_id) throws Exception {
        if (checkId(user_id) == 0)
            throw new Exception("Invalid Id");
        try {
            return userRepository.selectById(user_id);
        } catch (Exception e) {
            throw new Exception("Database Error");
        }
    }

    public int checkEmail(String email) throws Exception {
        try {
            return userRepository.checkEmail(email);
        } catch (Exception exception) {
            log.warn(exception.getMessage());
            throw new Exception("Database Error");
        }
    }


    public int checkId(Long id) throws Exception {
        try {
            return userRepository.checkId(id);
        } catch (Exception exception) {
            log.warn(exception.getMessage());
            throw new Exception("Database Error");
        }
    }
}