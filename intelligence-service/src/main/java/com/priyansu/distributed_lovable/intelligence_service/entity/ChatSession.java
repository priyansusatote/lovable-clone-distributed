package com.priyansu.distributed_lovable.intelligence_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "chat_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level =  AccessLevel.PRIVATE)

public class ChatSession {

    @EmbeddedId
    ChatSessionId id; // has projectId, userId (both are Uniquely identifies) ,  PK is (project_id, user_id)


    @CreationTimestamp
    @Column(nullable = false ,updatable = false)
    Instant createdAt;

    @UpdateTimestamp
    Instant updatedAt;

    Instant deletedAt;
}
