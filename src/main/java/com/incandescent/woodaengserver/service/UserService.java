package com.incandescent.woodaengserver.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.incandescent.woodaengserver.domain.Point;
import com.incandescent.woodaengserver.domain.User;
import com.incandescent.woodaengserver.dto.*;
import com.incandescent.woodaengserver.dto.TrophyInfo;
import com.incandescent.woodaengserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserProvider userProvider;
    private final UserRepository userRepository;
    @Value("${application.bucket.name}")
    private String bucketName;
    @Autowired
    private AmazonS3 s3Client;

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


    public void updateProfile(UpdateProfileRequest updateProfileRequest, MultipartFile image) {
        File file = new File(image.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(image.getBytes());
        } catch (IOException e) {
            log.error("Error converting multipartFile to file", e);
        }

        String fileName = updateProfileRequest.getId() + "_profile";
        s3Client.deleteObject(new DeleteObjectRequest(bucketName, fileName));
        s3Client.putObject(new PutObjectRequest(bucketName, fileName, file));
        file.delete();

        userRepository.saveProfile(new UserProfileResponse(
                updateProfileRequest.getId(),
                updateProfileRequest.getNickname(),
                s3Client.getUrl(bucketName, fileName).toString(),
                updateProfileRequest.getDog_name(),
                updateProfileRequest.getDog_age(),
                updateProfileRequest.getDog_breed(),
                updateProfileRequest.getDog_sex()
        ));
    }

    public UserProfileResponse getProfile(Long id) {
        UserProfileResponse userProfileResponse = userRepository.selectProfileById(id);

        String url = s3Client.getUrl(bucketName, userProfileResponse.getImage_id()).toString();
        return new UserProfileResponse(userProfileResponse.getId(), userProfileResponse.getNickname(),
                url, userProfileResponse.getDog_name(), userProfileResponse.getDog_age(), userProfileResponse.getDog_breed(),
                userProfileResponse.getDog_sex());
    }

    public boolean checkNickname(String nickname) {
        return userRepository.checkNickname(nickname);
    }
    public String getProfileImage(Long id) {
        return s3Client.getUrl(bucketName, userRepository.selectImageById(id)).toString();
    }
    
    public int getPoint(Long id) {
        return userRepository.getPoint(id);
    }

    public List<Point> getPointList(Long id) {
        return userRepository.getPointList(id);
    }
    
    public List<GameRecordInfo> getGameRecord(Long id) {
        return userRepository.getGameRecord(id);
    }

    public List<Ranking> getRankingList() {
        return userRepository.getRankingList();
    }

    public int getMyRanking(Long id) {
        return userRepository.getMyRank(id);
    }


    public TrophyInfo getTrophy(Long id) {
        return userRepository.getTrophy(id);
    }

    public Long getIdFromEmail(String email) throws Exception {
        return userProvider.retrieveByEmail(email).getId();
    }
}