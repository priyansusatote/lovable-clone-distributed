package com.priyansu.distributed_lovable.workspace_service.consumer;

import com.priyansu.distributed_lovable.common_lib.event.FileStoreRequestEvent;
import com.priyansu.distributed_lovable.workspace_service.service.ProjectFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileStorageConsumer {

    private final ProjectFileService projectFileService;

    @KafkaListener(topics = "file-storage-request-event", groupId = "workspace-group")
    public void consumeFileEvent(FileStoreRequestEvent requestEvent) {
        log.info("Received FileStoreRequestEvent from Kafka topic, So Saving file: {}", requestEvent.filePath());
        projectFileService.saveFile(requestEvent.projectId(), requestEvent.filePath(), requestEvent.content());
    }
}
