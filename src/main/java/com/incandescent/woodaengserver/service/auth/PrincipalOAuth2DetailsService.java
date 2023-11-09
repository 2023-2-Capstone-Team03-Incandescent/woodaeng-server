package com.incandescent.woodaengserver.service.auth;

import com.incandescent.woodaengserver.dto.PrincipalDetails;
import com.incandescent.woodaengserver.dto.oauth2.GoogleUserInfo;
import com.incandescent.woodaengserver.dto.oauth2.KakaoUserInfo;
import com.incandescent.woodaengserver.dto.oauth2.NaverUserInfo;
import com.incandescent.woodaengserver.dto.oauth2.OAuth2UserInfo;
import com.incandescent.woodaengserver.domain.User;
import com.incandescent.woodaengserver.service.UserProvider;
import com.incandescent.woodaengserver.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class PrincipalOAuth2DetailsService extends DefaultOAuth2UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserProvider userProvider;
    private final UserService userService;

    @Autowired
    public PrincipalOAuth2DetailsService(PasswordEncoder passwordEncoder, UserProvider userProvider, UserService userService) {
        this.passwordEncoder = passwordEncoder;
        this.userProvider = userProvider;
        this.userService = userService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        OAuth2UserInfo oAuth2UserInfo = null;

        if (userRequest.getClientRegistration().getRegistrationId().equals("google"))
            oAuth2UserInfo = new GoogleUserInfo(oAuth2User.getAttributes());
        else if (userRequest.getClientRegistration().getRegistrationId().equals("naver"))
            oAuth2UserInfo = new NaverUserInfo((Map)oAuth2User.getAttributes().get("response"));
        else if (userRequest.getClientRegistration().getRegistrationId().equals("kakao"))
            oAuth2UserInfo = new KakaoUserInfo(oAuth2User.getAttributes());
        else
            System.out.println("NOT SUPPORTED");

        String nickname = oAuth2UserInfo.getName();
        String email = oAuth2UserInfo.getEmail();
        String password = passwordEncoder.encode(email);
        String role = "ROLE_USER";
        String provider = oAuth2UserInfo.getProvider();
        String provider_id = oAuth2UserInfo.getProviderId();

        User user;

        try {
            if ( userProvider.checkEmail(email) == 0) {
                user = new User(email, nickname, password, role, provider, provider_id);
                userService.createUser(user);
            }else {
                user = userProvider.retrieveByEmail(email);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return new PrincipalDetails(user, oAuth2User.getAttributes());
    }
}