package com.priyansu.distributed_lovable.account_service.mapper;


import com.priyansu.distributed_lovable.account_service.dto.auth.SignupRequest;
import com.priyansu.distributed_lovable.account_service.dto.auth.UserProfileResponse;

import com.priyansu.distributed_lovable.account_service.entity.User;
import com.priyansu.distributed_lovable.common_lib.dto.UserDto;
import com.priyansu.distributed_lovable.common_lib.security.JwtUserPrincipal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toUser(SignupRequest signupRequest);  ///convert Signup req to User

    @Mapping(source = "userId", target = "id")
    UserProfileResponse toUserProfileResponse(JwtUserPrincipal user);

    UserDto toUserDto(User user);
}
