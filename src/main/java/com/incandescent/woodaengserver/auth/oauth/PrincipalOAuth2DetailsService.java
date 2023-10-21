package com.incandescent.woodaengserver.auth.oauth;

import com.incandescent.woodaengserver.auth.PrincipalDetails;
import com.incandescent.woodaengserver.auth.oauth.provider.GoogleUserInfo;
import com.incandescent.woodaengserver.auth.oauth.provider.KakaoUserInfo;
import com.incandescent.woodaengserver.auth.oauth.provider.NaverUserInfo;
import com.incandescent.woodaengserver.auth.oauth.provider.OAuth2UserInfo;
import com.incandescent.woodaengserver.user.UserProvider;
import com.incandescent.woodaengserver.user.UserService;
import com.incandescent.woodaengserver.user.model.User;
import com.incandescent.woodaengserver.util.BaseException;
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
                log.info("소셜 로그인이 최초입니다. 회원가입을 진행합니다.");
                user = new User(email, nickname, password, role, provider, provider_id);
                userService.createUser(user);
            }else {
                log.info("소셜 로그인 기록이 있습니다.");
                user = userProvider.retrieveByEmail(email);
            }
        } catch (BaseException e) {
            throw new RuntimeException(e);
        }

        return new PrincipalDetails(user, oAuth2User.getAttributes());
    }
}