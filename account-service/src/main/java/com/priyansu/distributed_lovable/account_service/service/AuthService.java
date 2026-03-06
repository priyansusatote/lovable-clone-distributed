package com.priyansu.distributed_lovable.account_service.service;


import com.priyansu.distributed_lovable.account_service.dto.auth.AuthResponse;
import com.priyansu.distributed_lovable.account_service.dto.auth.LoginRequest;
import com.priyansu.distributed_lovable.account_service.dto.auth.SignupRequest;

public interface AuthService {

     AuthResponse singup(SignupRequest request);

     AuthResponse login(LoginRequest request);
}
