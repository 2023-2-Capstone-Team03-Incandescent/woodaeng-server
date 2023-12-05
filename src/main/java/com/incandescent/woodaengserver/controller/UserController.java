package com.incandescent.woodaengserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incandescent.woodaengserver.dto.UpdateProfileRequest;
import com.incandescent.woodaengserver.dto.UserProfileResponse;
import com.incandescent.woodaengserver.service.UserService;
import com.incandescent.woodaengserver.service.auth.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/myprofile")
    public ResponseEntity getProfile(@RequestHeader("accessToken") String accessToken) {
        try {
            Long id = jwtTokenProvider.getUseridFromAcs(accessToken);
            UserProfileResponse userProfileResponse = userService.getProfile(id);
            return ResponseEntity.status(HttpStatus.OK).body(userProfileResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/{nickname}")
    public ResponseEntity checkNickname(@PathVariable("nickname") String nickname) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.checkNickname(nickname));
    }

    @GetMapping("/image")
    public ResponseEntity getProfileImage(@RequestHeader("accessToken") String accessToken) {
        Long id = jwtTokenProvider.getUseridFromAcs(accessToken);
        String image_url = userService.getProfileImage(id);
        return ResponseEntity.status(HttpStatus.OK).body(image_url);
    }

    @PostMapping("/profile")
    public ResponseEntity updateProfile(@RequestHeader("accessToken") String accessToken, @RequestParam("profile") String profileJson, @RequestParam("image") MultipartFile image) {
        try {
                UpdateProfileRequest updateProfileRequest = new ObjectMapper().readValue(profileJson, UpdateProfileRequest.class);
                Long id = jwtTokenProvider.getUseridFromAcs(accessToken);
                updateProfileRequest.setId(id);
                userService.updateProfile(updateProfileRequest, image);
                return ResponseEntity.status(HttpStatus.OK).build();
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/profile/{email}")
    public ResponseEntity registProfile(@PathVariable("email") String email, @RequestParam("profile") String profileJson, @RequestParam("image") MultipartFile image) {
        try {
            UpdateProfileRequest updateProfileRequest = new ObjectMapper().readValue(profileJson, UpdateProfileRequest.class);
            Long id = userService.getIdFromEmail(email);
            updateProfileRequest.setId(id);
            userService.updateProfile(updateProfileRequest, image);
            return ResponseEntity.status(HttpStatus.OK).build();
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/point")
    public ResponseEntity getPoint(@RequestHeader("accessToken") String accessToken) {
        Long id = jwtTokenProvider.getUseridFromAcs(accessToken);
        return ResponseEntity.status(HttpStatus.OK).body(userService.getPoint(id));
    }

    @GetMapping("/point/list")
    public ResponseEntity getPointList(@RequestHeader("accessToken") String accessToken) {
        Long id = jwtTokenProvider.getUseridFromAcs(accessToken);
        return ResponseEntity.status(HttpStatus.OK).body(userService.getPointList(id));
    }

    @GetMapping("/gameRecord")
    public ResponseEntity getGameRecord(@RequestHeader("accessToken") String accessToken) {
        Long id = jwtTokenProvider.getUseridFromAcs(accessToken);
        return ResponseEntity.status(HttpStatus.OK).body(userService.getGameRecord(id));
    }

    @GetMapping("/ranking")
    public ResponseEntity getRanking() {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getRankingList());
    }

    @GetMapping("myRank")
    public ResponseEntity getMyRank(@RequestHeader("accessToken") String accessToken) {
        Long id = jwtTokenProvider.getUseridFromAcs(accessToken);
        return ResponseEntity.status(HttpStatus.OK).body(userService.getMyRanking(id));
    }

    @GetMapping("/trophy")
    public ResponseEntity getTrophy(@RequestHeader("accessToken") String accessToken) {
        Long id = jwtTokenProvider.getUseridFromAcs(accessToken);
        return ResponseEntity.status(HttpStatus.OK).body(userService.getTrophy(id));
    }
}