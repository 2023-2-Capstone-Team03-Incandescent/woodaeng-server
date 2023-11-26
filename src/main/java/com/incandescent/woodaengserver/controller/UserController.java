package com.incandescent.woodaengserver.controller;

import com.incandescent.woodaengserver.service.UserProvider;
import com.incandescent.woodaengserver.dto.UserProfileResponse;
import com.incandescent.woodaengserver.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserProvider userProvider;

    @Autowired
    public UserController(UserProvider userProvider) {
        this.userProvider = userProvider;
    }

    @GetMapping({"","/"})
    public ResponseEntity getProfile(@RequestParam Long id) {
        User user;
        try {
            user = userProvider.retrieveById(id);
            UserProfileResponse userProfileResponse = new UserProfileResponse(user.getNickname());
            return ResponseEntity.status(HttpStatus.OK).body(userProfileResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}