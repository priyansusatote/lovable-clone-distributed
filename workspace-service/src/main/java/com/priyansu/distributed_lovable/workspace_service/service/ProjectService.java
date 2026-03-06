package com.priyansu.distributed_lovable.workspace_service.service;



import com.priyansu.distributed_lovable.common_lib.enums.ProjectPermissions;
import com.priyansu.distributed_lovable.workspace_service.dto.project.ProjectRequest;
import com.priyansu.distributed_lovable.workspace_service.dto.project.ProjectResponse;
import com.priyansu.distributed_lovable.workspace_service.dto.project.ProjectSummeryResponse;

import java.util.List;


public interface ProjectService {
     List<ProjectSummeryResponse> getUserProject();

     ProjectSummeryResponse getProjectById(Long id);

     ProjectResponse createProject(ProjectRequest request);

     ProjectResponse updateProject(Long id, ProjectRequest request);

    void softDelete(Long id);

    boolean hasPermission(Long projectId, ProjectPermissions permission);
}
