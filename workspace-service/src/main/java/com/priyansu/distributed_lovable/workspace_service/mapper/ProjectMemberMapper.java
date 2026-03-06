package com.priyansu.distributed_lovable.workspace_service.mapper;


import com.priyansu.distributed_lovable.workspace_service.dto.member.MemberResponse;
import com.priyansu.distributed_lovable.workspace_service.entity.ProjectMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProjectMemberMapper {
    //to match source and target field
    @Mapping(source = "projectRole",    target = "role")
    @Mapping(source = "id.userId",      target = "userId")
    MemberResponse toProjectMemberResponseFromMember(ProjectMember pm);

    List<MemberResponse> toMemberResponseListFromProjectMember(List<ProjectMember> members);
}
