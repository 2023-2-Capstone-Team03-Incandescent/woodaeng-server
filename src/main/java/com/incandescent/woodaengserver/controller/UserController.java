package com.incandescent.woodaengserver.controller;

import com.incandescent.woodaengserver.service.UserProvider;
import com.incandescent.woodaengserver.dto.UserProfileResponse;
import com.incandescent.woodaengserver.domain.User;
import com.incandescent.woodaengserver.util.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
            UserProfileResponse userProfileResponse = new UserProfileResponse(user.getNickname(), user.getIntroduce());
            return ResponseEntity.ok(userProfileResponse);
        } catch (CustomException e) {
            return ResponseEntity.badRequest().body(e.getStatus());
        }
    }
}