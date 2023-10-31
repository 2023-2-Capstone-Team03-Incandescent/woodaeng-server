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
public class UserService {

    private final UserProvider userProvider;
    private final UserRepository userRepository;

    @Autowired
    public UserService( UserProvider userProvider, UserRepository userRepository) {
        this.userProvider = userProvider;
        this.userRepository = userRepository;
    }

    public User createUser(User user) throws CustomException {
        if (userProvider.checkEmail(user.getEmail()) == 1)
            throw new CustomException(POST_USERS_EXISTS_EMAIL);
        try {
            return this.userRepository.insertUser(user);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CustomException(DATABASE_ERROR);
        }
    }
}