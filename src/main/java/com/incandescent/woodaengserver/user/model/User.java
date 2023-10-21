package com.incandescent.woodaengserver.user.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Long id;
    private String email;
    private String nickname;
    private String password;
    private String introduce;
    private String role;
    private String provider;
    private String provider_id;

    public User(Long id, String email, String nickname, String password, String role, String provider, String provider_id){
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.password = password;
        this.role = role;
        this.provider = provider;
        this.provider_id = provider_id;
    }

    public User(String email, String nickname, String password, String role, String provider, String provider_id){
        this.email = email;
        this.nickname = nickname;
        this.password = password;
        this.role = role;
        this.provider = provider;
        this.provider_id = provider_id;
    }
}