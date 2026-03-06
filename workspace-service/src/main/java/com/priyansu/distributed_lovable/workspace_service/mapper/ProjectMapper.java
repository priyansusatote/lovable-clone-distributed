package com.priyansu.distributed_lovable.workspace_service.mapper;


import com.priyansu.distributed_lovable.common_lib.enums.ProjectRole;
import com.priyansu.distributed_lovable.workspace_service.dto.project.ProjectResponse;
import com.priyansu.distributed_lovable.workspace_service.dto.project.ProjectSummeryResponse;
import com.priyansu.distributed_lovable.workspace_service.entity.Project;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProjectMapper {


    ProjectResponse toResponse(Project project);   // converts Project (entity) to ProjectResponse (DTO)

    ProjectSummeryResponse toProjectSummeryResponse(Project project, ProjectRole role);

    List<ProjectSummeryResponse> toProjectResponse(List<Project> projects);
}
