package com.priyansu.distributed_lovable.common_lib.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@RequiredArgsConstructor
@Getter
public enum ProjectRole {  //each role have Set of Permission
    //mapping Role -> to all permission they have
    EDITOR(Set.of(ProjectPermissions.EDIT, ProjectPermissions.VIEW, ProjectPermissions.VIEW_MEMBERS ,ProjectPermissions.DELETE)),
    VIEWER(Set.of(ProjectPermissions.VIEW, ProjectPermissions.VIEW_MEMBERS)),
    OWNER(Set.of(ProjectPermissions.VIEW, ProjectPermissions.EDIT, ProjectPermissions.DELETE,ProjectPermissions.VIEW_MEMBERS, ProjectPermissions.MANAGE_MEMBERS));

    private final Set<ProjectPermissions> permissions;
}
