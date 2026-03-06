package com.priyansu.distributed_lovable.common_lib.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ProjectPermissions {
    VIEW("project:view"),     //here (String Value Attached with enum) in value bracket (Resource : Action)
    EDIT("project:edit"),
    DELETE("project:delete"),
    MANAGE_MEMBERS("project_members:manage"),
    VIEW_MEMBERS("project_members:view");

    private final String value;
}
