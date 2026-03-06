package com.priyansu.distributed_lovable.workspace_service.security;

import com.priyansu.distributed_lovable.common_lib.enums.ProjectPermissions;
import com.priyansu.distributed_lovable.common_lib.security.AuthUtil;

import com.priyansu.distributed_lovable.workspace_service.repository.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityExpression {  //for Custom Bean Security Check (of  Security Method @PreAutherize)

    private final AuthUtil authUtil;
    private final ProjectMemberRepository projectMemberRepository;


    //code is repetitive so to reduce, created separate method and just pass parameter
    public boolean hasPermission(Long projectId, ProjectPermissions projectPermissions) {
        Long userId = authUtil.getCurrentUserId();

        //return if role matches else return false
        return projectMemberRepository.findRoleByProjectIdAndUserId(projectId, userId)
                .map(role -> role.getPermissions().contains(projectPermissions))  //permission based not role based (means u must have VIEW permission to view ThisMethod   // projectPermissions can be -> ProjectPermissions.VIEW  ,Edit,Delete,..
                .orElse(false);
    }

    //reduced code due to hasPermission

    public boolean canViewProject(Long projectId) {  //user should part of the project members table
      return hasPermission(projectId, ProjectPermissions.VIEW);

    }

    public boolean canEditProject(Long projectId) {
        return hasPermission(projectId, ProjectPermissions.EDIT);
    }

    public boolean canDeleteProject(Long projectId) {
        return hasPermission(projectId, ProjectPermissions.DELETE);
    }

    public boolean canViewMembers(Long projectId) {
        return hasPermission(projectId, ProjectPermissions.VIEW_MEMBERS);
    }

    public boolean canManageMembers(Long projectId) {
        return hasPermission(projectId, ProjectPermissions.MANAGE_MEMBERS);
    }

}
