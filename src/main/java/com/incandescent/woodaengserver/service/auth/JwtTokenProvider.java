package com.incandescent.woodaengserver.service.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Calendar;
import java.util.Date;

@Component
@Getter
public class JwtTokenProvider {
    private final long JWT_ACCESS_TOKEN_EXPTIME;
    private final long JWT_REFRESH_TOKEN_EXPTIME;
    private final String  JWT_ACCESS_SECRET_KEY;
    private final String  JWT_REFRESH_SECRET_KEY;
    private Key accessKey;
    private Key refreshKey;


    @Autowired
    public JwtTokenProvider(
            @Value("${jwt.time.access}") long JWT_ACCESS_TOKEN_EXPTIME,
            @Value("${jwt.time.refresh}") long JWT_REFRESH_TOKEN_EXPTIME,
            @Value("${jwt.secret.access}") String JWT_ACCESS_SECRET_KEY,
            @Value("${jwt.secret.refresh}") String JWT_REFRESH_SECRET_KEY) {
        this.JWT_ACCESS_TOKEN_EXPTIME = JWT_ACCESS_TOKEN_EXPTIME;
        this.JWT_REFRESH_TOKEN_EXPTIME = JWT_REFRESH_TOKEN_EXPTIME;
        this.JWT_ACCESS_SECRET_KEY = JWT_ACCESS_SECRET_KEY;
        this.JWT_REFRESH_SECRET_KEY = JWT_REFRESH_SECRET_KEY;
    }


    @PostConstruct
    public void initialize() {
        byte[] accessKeyBytes = Decoders.BASE64.decode(JWT_ACCESS_SECRET_KEY);
        this.accessKey = Keys.hmacShaKeyFor(accessKeyBytes);

        byte[] secretKeyBytes = Decoders.BASE64.decode(JWT_REFRESH_SECRET_KEY);
        this.refreshKey = Keys.hmacShaKeyFor(secretKeyBytes);
    }

    public String createAccessToken(Long userid) {
        Claims claims = Jwts.claims().setSubject(userid.toString());

        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.add(Calendar.SECOND, (int) JWT_ACCESS_TOKEN_EXPTIME);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
//                .setExpiration(new Date(now.getTime()  + JWT_ACCESS_TOKEN_EXPTIME))
                .setExpiration(cal.getTime())
                .signWith(accessKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(Long userid) {
        Claims claims = Jwts.claims().setSubject(userid.toString());

        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.add(Calendar.SECOND, (int) JWT_REFRESH_TOKEN_EXPTIME);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
//                .setExpiration(new Date(now.getTime()  + JWT_REFRESH_TOKEN_EXPTIME))
                .setExpiration(cal.getTime())
                .signWith(accessKey, SignatureAlgorithm.HS256)
                .compact();
    }

//    // JWT 토큰에서 인증 정보 조회
//    public Authentication getAuthentication(String token) {
//        PrincipalDetails userDetails = (PrincipalDetails) userDetailsService.loadUserByUsername(this.getUserid(token));
//        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
//    }

    public Long getUseridFromAcs(String token) {
        return Long.valueOf(Jwts.parserBuilder().setSigningKey(accessKey).build()
                .parseClaimsJws(token).getBody().getSubject());
    }

    public Long getUseridFromRef(String token) {
        return Long.valueOf(Jwts.parserBuilder().setSigningKey(accessKey).build()
                .parseClaimsJws(token).getBody().getSubject());
    }

    public Long getExpiration(String accessToken) {
        Date expiration = Jwts.parserBuilder().setSigningKey(accessKey).build()
                .parseClaimsJws(accessToken).getBody().getExpiration();
        Long now = new Date().getTime();
        return expiration.getTime() - now;
    }
}