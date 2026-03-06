package com.priyansu.distributed_lovable.workspace_service.repository;


import com.priyansu.distributed_lovable.common_lib.enums.ProjectRole;
import com.priyansu.distributed_lovable.workspace_service.entity.ProjectMember;
import com.priyansu.distributed_lovable.workspace_service.entity.ProjectMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {

    List<ProjectMember> findByIdProjectId(Long projectId);

    Optional<ProjectMember> findByIdProjectIdAndIdUserId(Long projectId, Long userId);

    void deleteById(ProjectMemberId id);

    @Query("""
       SELECT pm.projectRole 
       FROM ProjectMember pm 
       WHERE pm.id.projectId = :pId 
         AND pm.id.userId = :uId
       """)
    Optional<ProjectRole> findRoleByProjectIdAndUserId(
            @Param("pId") Long projectId,          //@param used to pass/match field name in Query( :projectId or :pId)
            @Param("uId") Long userId
    );



    @Query("""
            SELECT COUNT(pm) FROM ProjectMember pm
            WHERE pm.id.userId = :userId AND pm.projectRole = 'OWNER'
            """)
    int countProjectOwnedByUser(@Param("userId") Long userId);
}
