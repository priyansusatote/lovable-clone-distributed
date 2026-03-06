package com.priyansu.distributed_lovable.workspace_service.controller;


import com.priyansu.distributed_lovable.common_lib.security.AuthUtil;
import com.priyansu.distributed_lovable.workspace_service.dto.member.InviteMemberRequest;
import com.priyansu.distributed_lovable.workspace_service.dto.member.MemberResponse;
import com.priyansu.distributed_lovable.workspace_service.dto.member.UpdateMemberRoleRequest;
import com.priyansu.distributed_lovable.workspace_service.service.ProjectMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/projects/{projectId}/members")
public class ProjectMemberController {
    //DI
    private final ProjectMemberService projectMemberService;
    private final AuthUtil authUtil;

    @GetMapping
    public ResponseEntity<List<MemberResponse>> getProjectMember(@PathVariable Long projectId){

        return ResponseEntity.ok(projectMemberService.getProjectMember(projectId));
    }

    @PostMapping
    public ResponseEntity<MemberResponse> inviteMember(@PathVariable Long projectId, @RequestBody @Valid InviteMemberRequest request){

        return ResponseEntity.status(HttpStatus.CREATED).body(projectMemberService.inviteMember(projectId,request));
    }

    @PatchMapping("/{memberId}")
    public ResponseEntity<MemberResponse> updateMemberRole(
            @PathVariable Long projectId,
            @PathVariable Long memberId,
            @RequestBody @Valid UpdateMemberRoleRequest request){

        return ResponseEntity.ok(projectMemberService.updateMemberRole(projectId, memberId, request));
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> removeProjectMember(@PathVariable Long projectId, @PathVariable Long memberId){

        projectMemberService.removeProjectMemberRole(projectId, memberId);
        return ResponseEntity.noContent().build();
    }
}
