package com.incandescent.woodaengserver.controller;

import com.incandescent.woodaengserver.dto.UpdateProfileRequest;
import com.incandescent.woodaengserver.service.UserProvider;
import com.incandescent.woodaengserver.dto.UserProfileResponse;
import com.incandescent.woodaengserver.domain.User;
import com.incandescent.woodaengserver.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserProvider userProvider;
    private final UserService userService;

    @Autowired
    public UserController(UserProvider userProvider, UserService userService) {
        this.userProvider = userProvider;
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public ResponseEntity getProfile(@PathVariable("id") Long id) {
        User user;
        try {
//            user = userProvider.retrieveById(id);
//            UserProfileResponse userProfileResponse = new UserProfileResponse(user.getId());\
            UserProfileResponse userProfileResponse = userService.getProfile(id);
            return ResponseEntity.status(HttpStatus.OK).body(userProfileResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/image/{id}")
    public ResponseEntity getProfileImage(@PathVariable("id") Long id) {
        String image_url = userService.getProfileImage(id);
        return ResponseEntity.status(HttpStatus.OK).body(image_url);
    }

    @PostMapping("/profile")
    public ResponseEntity updateProfile(@RequestBody UpdateProfileRequest updateProfileRequest) {
        try {
            userService.updateProfile(updateProfileRequest);
            return ResponseEntity.status(HttpStatus.OK).build();
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/point/{id}")
    public ResponseEntity getPoint(@PathVariable("id") Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getPoint(id));
    }

    @GetMapping("/point/list/{id}")
    public ResponseEntity getPointList(@PathVariable("id") Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getPointList(id));
    }

    @GetMapping("/gameRecord/{id}")
    public ResponseEntity getGameRecord(@PathVariable("id") Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getGameRecord(id));
    }

    @GetMapping("/ranking/{id}")
    public ResponseEntity getRanking(@PathVariable("id") Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getRanking(id));
    }

    @GetMapping("/trophy/{id}")
    public ResponseEntity getTrophy(@PathVariable("id") Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getTrophy(id));
    }
}