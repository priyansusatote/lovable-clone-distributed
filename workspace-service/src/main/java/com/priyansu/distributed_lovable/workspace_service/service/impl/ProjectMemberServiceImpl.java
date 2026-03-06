package com.priyansu.distributed_lovable.workspace_service.service.impl;


import com.priyansu.distributed_lovable.common_lib.dto.UserDto;
import com.priyansu.distributed_lovable.common_lib.enums.ProjectRole;
import com.priyansu.distributed_lovable.common_lib.exception.ForbiddenException;
import com.priyansu.distributed_lovable.common_lib.exception.ResourceNotFoundException;
import com.priyansu.distributed_lovable.common_lib.security.AuthUtil;
import com.priyansu.distributed_lovable.workspace_service.client.AccountClient;
import com.priyansu.distributed_lovable.workspace_service.dto.member.InviteMemberRequest;
import com.priyansu.distributed_lovable.workspace_service.dto.member.MemberResponse;
import com.priyansu.distributed_lovable.workspace_service.dto.member.UpdateMemberRoleRequest;
import com.priyansu.distributed_lovable.workspace_service.entity.Project;
import com.priyansu.distributed_lovable.workspace_service.entity.ProjectMember;
import com.priyansu.distributed_lovable.workspace_service.entity.ProjectMemberId;
import com.priyansu.distributed_lovable.workspace_service.mapper.ProjectMemberMapper;
import com.priyansu.distributed_lovable.workspace_service.repository.ProjectMemberRepository;
import com.priyansu.distributed_lovable.workspace_service.repository.ProjectRepository;
import com.priyansu.distributed_lovable.workspace_service.service.ProjectMemberService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ProjectMemberServiceImpl implements ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberMapper projectMemberMapper;
    private final AuthUtil authUtil;
    private final AccountClient accountClient;

    //Get all members list (only if user requesting this list is the owner)
    @Override
    @PreAuthorize("@securityExpression.canViewMembers(#projectId)")
    public List<MemberResponse> getProjectMember(Long projectId) {
        Long userId = authUtil.getCurrentUserId();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId.toString()));

        ProjectMember member = projectMemberRepository
                .findByIdProjectIdAndIdUserId(projectId, userId)
                .orElseThrow(() -> new ForbiddenException("Not authorized"));



        var members = projectMemberRepository.findByIdProjectId(projectId);  //get all the members stored in the db for this project


        //covert Entity (ProjectMember) to dto (MemberResponse) by Mapper
        return projectMemberMapper.toMemberResponseListFromProjectMember(members);
    }

    @Override
    @PreAuthorize("@securityExpression.canManageMembers(#projectId)")
    public MemberResponse inviteMember(Long projectId, InviteMemberRequest request) {
        Long userId = authUtil.getCurrentUserId();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId.toString()));


        UserDto userToInvite = accountClient.getUserByEmail(request.username())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.username()));

        if (userToInvite.id().equals(userId)) {
            throw new ForbiddenException("Cannot invite yourself");
        }

        ProjectMemberId projectMemberId = new ProjectMemberId(projectId, userToInvite.id());
        if (projectMemberRepository.existsById(projectMemberId)) {
            throw new ForbiddenException("User is already a member");
        }

        //add new member
        ProjectMember member = ProjectMember.builder()
                .id(projectMemberId)
                .project(project)
                .projectRole(request.role())
                .invitedAt(Instant.now())
                .build();

        return projectMemberMapper.toProjectMemberResponseFromMember(projectMemberRepository.save(member));
    }

    @Override
    @PreAuthorize("@securityExpression.canManageMembers(#projectId)")
    public MemberResponse updateMemberRole(Long projectId, Long memberId, UpdateMemberRoleRequest request) {
        Long userId = authUtil.getCurrentUserId();

        // 1️⃣ Load the project
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId.toString()));


        //owner cannot downgrade their role
        if (memberId.equals(userId)) {
            throw new ForbiddenException("Owner cannot change their own role");
        }


        // 3️⃣ Load the TARGET member (NOT current user)
        ProjectMember member = projectMemberRepository
                .findByIdProjectIdAndIdUserId(projectId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("user not a Member", memberId.toString()));

        // 4️⃣ Update the role
        member.setProjectRole(request.role());

        // 5️⃣ Save and return
        projectMemberRepository.save(member);

        return projectMemberMapper.toProjectMemberResponseFromMember(member);
    }

    @Override
    @PreAuthorize("@securityExpression.canManageMembers(#projectId)")
    public void removeProjectMemberRole(Long projectId, Long memberId) {
        Long userId = authUtil.getCurrentUserId();


        // 1️⃣ Find project
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId.toString()));

        // 2️⃣ Only owner can remove members
        ProjectMember owner = projectMemberRepository
                .findByIdProjectIdAndIdUserId(projectId, userId)
                .orElseThrow(() -> new ForbiddenException("Not authorized"));

        if (owner.getProjectRole() != ProjectRole.OWNER) {
            throw new ForbiddenException("Only owner can remove members");
        }

        // 3️⃣ Owner cannot remove themselves
        if (memberId.equals(userId)) {
            throw new ForbiddenException("Owner cannot remove themselves");
        }


        // 4️⃣ Check if membership exists
        ProjectMember member = projectMemberRepository
                .findByIdProjectIdAndIdUserId(projectId, memberId)
                .orElseThrow(() ->new ResourceNotFoundException("user not a Member", memberId.toString()));

        // 5️⃣ Delete membership
        projectMemberRepository.delete(member);
    }
}
