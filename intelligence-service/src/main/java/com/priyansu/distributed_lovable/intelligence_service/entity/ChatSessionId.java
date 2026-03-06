package com.priyansu.distributed_lovable.intelligence_service.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Embeddable
@Getter
@Setter
@ToString
public class ChatSessionId implements Serializable {
    Long projectId;
    Long userId;
}
