package com.incandescent.woodaengserver.service;

import com.incandescent.woodaengserver.domain.User;
import com.incandescent.woodaengserver.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class UserService {

    private final UserProvider userProvider;
    private final UserRepository userRepository;

    @Autowired
    public UserService( UserProvider userProvider, UserRepository userRepository) {
        this.userProvider = userProvider;
        this.userRepository = userRepository;
    }

    public User createUser(User user) throws Exception {
        if (userProvider.checkEmail(user.getEmail()) == 1)
            throw new Exception("Email is already in use");
        try {
            return this.userRepository.insertUser(user);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("Database Error");
        }
    }
}