package com.priyansu.distributed_lovable.account_service.service.impl;


import com.priyansu.distributed_lovable.account_service.dto.auth.AuthResponse;
import com.priyansu.distributed_lovable.account_service.dto.auth.LoginRequest;
import com.priyansu.distributed_lovable.account_service.dto.auth.SignupRequest;
import com.priyansu.distributed_lovable.account_service.entity.User;
import com.priyansu.distributed_lovable.account_service.mapper.UserMapper;
import com.priyansu.distributed_lovable.account_service.repository.UserRepository;
import com.priyansu.distributed_lovable.account_service.service.AuthService;
import com.priyansu.distributed_lovable.common_lib.exception.BadRequestException;
import com.priyansu.distributed_lovable.common_lib.security.AuthUtil;
import com.priyansu.distributed_lovable.common_lib.security.JwtUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthUtil authUtil;
    private final AuthenticationManager authenticationManager;

    @Override
    public AuthResponse singup(SignupRequest request) {

        userRepository.findByUsername(request.username())
                .ifPresent(user -> {
                            throw new BadRequestException("User is already exists with username " + request.username());
                        }
                );

        User user = userMapper.toUser(request);
        //encode password
        user.setPassword(passwordEncoder.encode(request.password()));

        user = userRepository.save(user);

        JwtUserPrincipal jwtUserPrincipal = new JwtUserPrincipal(user.getId(), user.getName(),
                user.getUsername(), null, new ArrayList<>());

        String token = authUtil.generateAccessToken(jwtUserPrincipal); //create jwtToken

        return new AuthResponse(token, userMapper.toUserProfileResponse(jwtUserPrincipal));
    }

    @Override
    public AuthResponse login(LoginRequest request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        JwtUserPrincipal user = (JwtUserPrincipal) authentication.getPrincipal(); //take authenticated user Detail from above

        String token = authUtil.generateAccessToken(user); //create jwtToken

        return new AuthResponse(token, userMapper.toUserProfileResponse(user));
    }
}
