package com.priyansu.distributed_lovable.workspace_service.repository;

import com.priyansu.distributed_lovable.workspace_service.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessEventRepository extends JpaRepository<ProcessedEvent, String> {
}
