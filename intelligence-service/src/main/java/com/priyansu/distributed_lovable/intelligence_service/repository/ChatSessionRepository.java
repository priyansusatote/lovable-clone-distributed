package com.priyansu.distributed_lovable.intelligence_service.repository;


import com.priyansu.distributed_lovable.intelligence_service.entity.ChatSession;
import com.priyansu.distributed_lovable.intelligence_service.entity.ChatSessionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, ChatSessionId> {
}
