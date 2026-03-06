package com.priyansu.distributed_lovable.intelligence_service.repository;

import com.priyansu.distributed_lovable.intelligence_service.entity.ChatMessage;
import com.priyansu.distributed_lovable.intelligence_service.entity.ChatSession;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("""
    SELECT DISTINCT m FROM ChatMessage m
    LEFT JOIN FETCH m.events 
    WHERE m.chatSession = :chatSession
    ORDER BY m.createdAt ASC
""")
    List<ChatMessage> findByChatSession(ChatSession chatSession);
}
