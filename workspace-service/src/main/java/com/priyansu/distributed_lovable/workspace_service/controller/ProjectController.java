package com.priyansu.distributed_lovable.workspace_service.controller;

import com.priyansu.distributed_lovable.common_lib.security.AuthUtil;
import com.priyansu.distributed_lovable.workspace_service.dto.project.DeployResponse;
import com.priyansu.distributed_lovable.workspace_service.dto.project.ProjectRequest;
import com.priyansu.distributed_lovable.workspace_service.dto.project.ProjectResponse;
import com.priyansu.distributed_lovable.workspace_service.dto.project.ProjectSummeryResponse;
import com.priyansu.distributed_lovable.workspace_service.service.DeploymentService;
import com.priyansu.distributed_lovable.workspace_service.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/projects")

public class ProjectController {

    private final ProjectService projectService;
    private final AuthUtil authUtil;
    private final DeploymentService deploymentService;

    @GetMapping
    public ResponseEntity<List<ProjectSummeryResponse>> getMyProject(){

        return ResponseEntity.ok(projectService.getUserProject());  //getUserProject(method gives all project that userHave) of this user=>userID
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectSummeryResponse> getProjectById(@PathVariable Long id){

        return ResponseEntity.ok(projectService.getProjectById(id));
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@RequestBody @Valid ProjectRequest request){

        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(@PathVariable Long id, @RequestBody @Valid ProjectRequest request){

        return ResponseEntity.ok(projectService.updateProject(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id){

       projectService.softDelete(id);
       return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deploy")
    public ResponseEntity<DeployResponse> deployProject(@PathVariable Long id) {
        return ResponseEntity.ok(deploymentService.deploy(id));
    }


}
