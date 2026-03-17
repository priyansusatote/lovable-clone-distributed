package com.priyansu.distributed_lovable.workspace_service.consumer;

import com.priyansu.distributed_lovable.common_lib.event.FileStoreRequestEvent;
import com.priyansu.distributed_lovable.common_lib.event.FileStoreResponseEvent;
import com.priyansu.distributed_lovable.workspace_service.entity.ProcessedEvent;
import com.priyansu.distributed_lovable.workspace_service.repository.ProcessEventRepository;
import com.priyansu.distributed_lovable.workspace_service.service.ProjectFileService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileStorageConsumer {

    private final ProjectFileService projectFileService;
    private final ProcessEventRepository processEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    @KafkaListener(topics = "file-storage-request-event", groupId = "workspace-group")
    public void consumeFileEvent(FileStoreRequestEvent requestEvent) {

        //first check for Idempotency (if sagaId already Present)
        if (processEventRepository.existsById(requestEvent.sagaId())) {
            log.info("Duplicate Saga detected: {}. resending previous Acknowledgment.", requestEvent.sagaId());
            sendResponse(requestEvent, true, null);
            return;
        }

        try {
            log.info("Received FileStoreRequestEvent from Kafka topic, So Saving file: {}", requestEvent.filePath());
            projectFileService.saveFile(requestEvent.projectId(), requestEvent.filePath(), requestEvent.content());
            processEventRepository.save(new ProcessedEvent(
                    requestEvent.sagaId(), LocalDateTime.now()
            ));

            sendResponse(requestEvent, true, null);

        } catch (Exception e) {
           log.error("error Saving File: {}", e.getMessage());
           sendResponse(requestEvent, false, e.getMessage());
        }

    }

    private void sendResponse(FileStoreRequestEvent req, boolean success, String error) {
        FileStoreResponseEvent response = FileStoreResponseEvent.builder()
                .sagaId(req.sagaId())
                .projectId(req.projectId())
                .success(success)
                .errorMessage(error)
                .build();

        kafkaTemplate.send("file-store-responses", response);
    }


}
