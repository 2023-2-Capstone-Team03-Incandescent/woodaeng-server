package com.incandescent.woodaengserver.user;

import com.incandescent.woodaengserver.user.UserProvider;
import com.incandescent.woodaengserver.user.dto.GetUserRes;
import com.incandescent.woodaengserver.user.model.User;
import com.incandescent.woodaengserver.util.BaseException;
import com.incandescent.woodaengserver.util.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserProvider userProvider;

    @Autowired
    public UserController(UserProvider userProvider) {
        this.userProvider = userProvider;
    }

    @GetMapping("")
    public BaseResponse<GetUserRes> getProfile(HttpServletRequest request) {
        try {
            Long user_id = Long.parseLong(request.getAttribute("user_id").toString());
            User user = userProvider.retrieveById(user_id);
            GetUserRes getUserRes = new GetUserRes(user.getNickname(), user.getIntroduce());
            return new BaseResponse<>(getUserRes);
        } catch (BaseException e) {
            return new BaseResponse<>(e.getStatus());
        }
    }
}