package com.priyansu.distributed_lovable.intelligence_service.security;

import com.priyansu.distributed_lovable.common_lib.enums.ProjectPermissions;
import com.priyansu.distributed_lovable.common_lib.security.AuthUtil;

import com.priyansu.distributed_lovable.intelligence_service.client.WorkspaceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityExpression {  //for Custom Bean Security Check (of  Security Method @PreAutherize)

    private final AuthUtil authUtil;
    private final WorkspaceClient workspaceClient;


    //code is repetitive so to reduce, created separate method and just pass parameter
    public boolean hasPermission(Long projectId, ProjectPermissions projectPermissions) {

        return workspaceClient.checkPermission(projectId, projectPermissions);
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
