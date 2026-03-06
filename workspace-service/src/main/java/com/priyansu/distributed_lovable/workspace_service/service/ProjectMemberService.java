package com.priyansu.distributed_lovable.workspace_service.service;



import com.priyansu.distributed_lovable.workspace_service.dto.member.InviteMemberRequest;
import com.priyansu.distributed_lovable.workspace_service.dto.member.MemberResponse;
import com.priyansu.distributed_lovable.workspace_service.dto.member.UpdateMemberRoleRequest;

import java.util.List;

public interface ProjectMemberService {
     List<MemberResponse> getProjectMember(Long projectId);

     MemberResponse inviteMember(Long projectId, InviteMemberRequest request);

     MemberResponse updateMemberRole(Long projectId, Long memberId, UpdateMemberRoleRequest request);

     void removeProjectMemberRole(Long projectId, Long memberId);
}
