package com.priyansu.distributed_lovable.workspace_service.service.impl;


import com.priyansu.distributed_lovable.common_lib.dto.PlanDto;
import com.priyansu.distributed_lovable.common_lib.enums.ProjectPermissions;
import com.priyansu.distributed_lovable.common_lib.enums.ProjectRole;
import com.priyansu.distributed_lovable.common_lib.exception.BadRequestException;
import com.priyansu.distributed_lovable.common_lib.exception.ResourceNotFoundException;
import com.priyansu.distributed_lovable.common_lib.security.AuthUtil;
import com.priyansu.distributed_lovable.workspace_service.client.AccountClient;
import com.priyansu.distributed_lovable.workspace_service.dto.project.ProjectRequest;
import com.priyansu.distributed_lovable.workspace_service.dto.project.ProjectResponse;
import com.priyansu.distributed_lovable.workspace_service.dto.project.ProjectSummeryResponse;
import com.priyansu.distributed_lovable.workspace_service.entity.Project;
import com.priyansu.distributed_lovable.workspace_service.entity.ProjectMember;
import com.priyansu.distributed_lovable.workspace_service.entity.ProjectMemberId;
import com.priyansu.distributed_lovable.workspace_service.mapper.ProjectMapper;
import com.priyansu.distributed_lovable.workspace_service.repository.ProjectMemberRepository;
import com.priyansu.distributed_lovable.workspace_service.repository.ProjectRepository;
import com.priyansu.distributed_lovable.workspace_service.security.SecurityExpression;
import com.priyansu.distributed_lovable.workspace_service.service.ProjectService;
import com.priyansu.distributed_lovable.workspace_service.service.ProjectTemplateService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    private final ProjectMemberRepository projectMemberRepository;
    private final AuthUtil authUtil;
    private final ProjectTemplateService projectTemplateService;
    private final AccountClient accountClient;
    private final SecurityExpression securityExpression;

    @Override
    public List<ProjectSummeryResponse> getUserProject() {
        Long userId = authUtil.getCurrentUserId();

        var projectWithRoles = projectRepository.findAllAccessibleByUser(userId);
        return projectWithRoles.stream() //map one by one projectWithRoles in to List of Response
                .map(p -> projectMapper.toProjectSummeryResponse(p.getProject(), p.getRole()))
                .toList();
    }

    @Override
    @PreAuthorize("@securityExpression.canViewProject(#projectId)") //first S->s should be always lower even Bean/className is not
    public ProjectSummeryResponse getProjectById(Long projectId) {
        Long userId = authUtil.getCurrentUserId();

        var projectWithRole = projectRepository.findAccessibleProjectByIdWithRole(projectId, userId)
                .orElseThrow(() -> new BadRequestException("Project with id " + projectId + " not found"));

/*       projectMemberRepository.findByProjectIdAndUserId(id, userId)
               .orElseThrow(() -> new ForbiddenException("Not authorized")); */   //service level Authorization check removed already done using method security @PreAuthrize


        return projectMapper.toProjectSummeryResponse(projectWithRole.getProject() , projectWithRole.getRole() );
    }

    @Override
    public ProjectResponse createProject(ProjectRequest request) {
        //authorization
        if(!canCreateProject()){
           throw  new BadRequestException("You are not allowed to create a new project with current Plan Upgrade now");
        }

        Long userId = authUtil.getCurrentUserId();

        // 1️⃣  the owner of the project (this is only for setting projectMember.user(owner) //Use getReferenceById() when you only need the entity to form a relationship, not its data.
      


        // 2️⃣ Create a new Project entity using the request data
        Project project = Project.builder()
                .name(request.name())
                .isPublic(false)
                .build();

        // 3️⃣ Save the newly created project into the database
        Project saved = projectRepository.save(project);

        //initialize starter project to the new created project (create a copy of template)
        projectTemplateService.initializeProjectFromTemplate(project.getId());

        //Whenever a Project is Created a "ProjectMember" also created
        ProjectMemberId projectMemberId = new ProjectMemberId(project.getId(), userId);
        ProjectMember projectMember = ProjectMember.builder()
                .id(projectMemberId)
                .projectRole(ProjectRole.OWNER)
                .acceptedAt(Instant.now())
                .invitedAt(Instant.now())
                .project(project)
                .build();
        projectMemberRepository.save(projectMember);

        // 4️⃣ Convert Entity → DTO using MapStruct and return response
        return projectMapper.toResponse(saved);

    }

    @Override
    @PreAuthorize("@securityExpression.canEditProject(#projectId)")
    public ProjectResponse updateProject(Long projectId, ProjectRequest request) { // "request" is dto getting from user
        Long userId = authUtil.getCurrentUserId();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId.toString()));


        //dto("request Dto")-> Entity
        project.setName(request.name());


        //save
        Project updated = projectRepository.save(project);

        return projectMapper.toResponse(updated); //Entity (updated) -> dto

    }

    @Override
    @PreAuthorize("@securityExpression.canDeleteProject(#projectId)")
    public void softDelete(Long projectId) {
        Long userId = authUtil.getCurrentUserId();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId.toString()));


        project.setDeletedAt(Instant.now());

        //save
        projectRepository.save(project);
    }

    private boolean canCreateProject() {
        Long userId = authUtil.getCurrentUserId();
        if (userId == null) {
            return false;
        }
        PlanDto plan = accountClient.getCurrentSubscribedPlanByUser();

        int maxAllowed = plan.maxProjects();
        int ownedCount = projectMemberRepository.countProjectOwnedByUser(userId);

        return ownedCount < maxAllowed;
    }


    @Override
    public boolean hasPermission(Long projectId, ProjectPermissions permission) {
        return securityExpression.hasPermission(projectId, permission);
    }

}

