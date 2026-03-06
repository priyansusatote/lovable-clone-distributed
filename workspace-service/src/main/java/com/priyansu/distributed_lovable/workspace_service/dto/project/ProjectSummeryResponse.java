package com.priyansu.distributed_lovable.workspace_service.dto.project;



import com.priyansu.distributed_lovable.common_lib.enums.ProjectRole;

import java.time.Instant;

public record ProjectSummeryResponse(
        Long id,
        String name,
        Instant createdAt,
        Instant updatedAt,
        ProjectRole role
) {
}
