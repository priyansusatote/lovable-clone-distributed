package com.priyansu.distributed_lovable.common_lib.security;


import com.priyansu.distributed_lovable.common_lib.dto.UserDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;

//This service acts as a utility class that isolates all cryptographic operations. It handles the low-level logic of interacting with the io.jsonwebtoken library

@Component
@RequiredArgsConstructor
public class AuthUtil {

    @Value("${jwt.secret-key}")
    private String jwtSecretKey;

    //method (to convert Simple string to SecretKey) we use Algo named:hmacShaKey
    private SecretKey getSecretKey() {

        return Keys.hmacShaKeyFor(jwtSecretKey.getBytes(StandardCharsets.UTF_8));
    }

    //generate jwtToken
    public String generateAccessToken(JwtUserPrincipal user) {
        return Jwts.builder()
                .subject(user.username())
                .claim("userId", user.userId())
                .claim("name", user.name())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 100)) //temporary increased for tasting
                .signWith(getSecretKey())
                .compact();
    }

    //Validate JWT, Extract user information, Check expiration, Ensure token is trusted (JJWT itself does the validation automatic)
    //Parse → library validates → extract → build principal
    public JwtUserPrincipal verifyAccessToken(String token) {
        //Parser = the thing that reads + validates the JWT // Claims = the DATA stored inside the JWT (like subject,role,userId, iat,exp
        Claims claims = Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)  //JJWT library Automatically validates -> verifies signature, expiration,token structure, throws exception if invalid/expired
                .getPayload();  //will get all the claims

        //extract the authenticated user’s identity from a verified JWT so the backend knows who is making the request.
        Long userId = claims.get("userId", Long.class);
        String name = claims.get("name", String.class);
        String username = claims.getSubject(); //we need username , we passed username in subject so that using here

        return  new JwtUserPrincipal(userId, name, username, null ,new ArrayList<>());
    }

    //get current user's userId
    public Long getCurrentUserId() {
        //gets the currently logged-in user for the current request. means :“Give me the authentication details of the user who is making this request.”
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();  //SecurityContextHolder = current request’s security info & "Authentication" = logged-in user

        if(authentication == null || !(authentication.getPrincipal() instanceof  JwtUserPrincipal)) {
            throw new AuthenticationCredentialsNotFoundException("Invalid username or password : No Jwt Found");
        }

        JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();

        return principal.userId();
    }
}
