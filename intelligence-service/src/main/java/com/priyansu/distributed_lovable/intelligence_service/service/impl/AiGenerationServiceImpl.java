package com.priyansu.distributed_lovable.intelligence_service.service.impl;


import com.priyansu.distributed_lovable.common_lib.enums.ChatEventStatus;
import com.priyansu.distributed_lovable.common_lib.enums.ChatEventType;
import com.priyansu.distributed_lovable.common_lib.enums.MessageRole;
import com.priyansu.distributed_lovable.common_lib.event.FileStoreRequestEvent;
import com.priyansu.distributed_lovable.common_lib.exception.ResourceNotFoundException;
import com.priyansu.distributed_lovable.common_lib.security.AuthUtil;
import com.priyansu.distributed_lovable.intelligence_service.client.WorkspaceClient;
import com.priyansu.distributed_lovable.intelligence_service.dto.chat.StreamResponse;
import com.priyansu.distributed_lovable.intelligence_service.entity.ChatEvent;
import com.priyansu.distributed_lovable.intelligence_service.entity.ChatMessage;
import com.priyansu.distributed_lovable.intelligence_service.entity.ChatSession;
import com.priyansu.distributed_lovable.intelligence_service.entity.ChatSessionId;
import com.priyansu.distributed_lovable.intelligence_service.llm.PromptUtils;
import com.priyansu.distributed_lovable.intelligence_service.llm.advisors.FileTreeContextAdvisor;
import com.priyansu.distributed_lovable.intelligence_service.llm.tools.CodeGenerationTools;
import com.priyansu.distributed_lovable.intelligence_service.llm.tools.LlmResponseParser;
import com.priyansu.distributed_lovable.intelligence_service.repository.ChatEventRepository;
import com.priyansu.distributed_lovable.intelligence_service.repository.ChatMessageRepository;
import com.priyansu.distributed_lovable.intelligence_service.repository.ChatSessionRepository;
import com.priyansu.distributed_lovable.intelligence_service.service.AiGenerationService;
import com.priyansu.distributed_lovable.intelligence_service.service.UsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiGenerationServiceImpl implements AiGenerationService {

    private final ChatClient chatClient;
    private final AuthUtil authUtil;
//    private final ProjectFileService projectFileService;
    private final FileTreeContextAdvisor fileTreeContextAdvisor;
    private final ChatSessionRepository chatSessionRepository;
//    private final ProjectRepository projectRepository;
//    private final UserRepository userRepository;
    private final LlmResponseParser llmResponseParser;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatEventRepository chatEventRepository;
    private final UsageService usageService;
    private final WorkspaceClient workspaceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final Pattern FILE_TAG_PATTERN = Pattern.compile("<file path=\"([^\"]+)\">(.*?)</file>", Pattern.DOTALL);

    @Override
    @PreAuthorize("@securityExpression.canEditProject(#projectId)")
    public Flux<StreamResponse> streamResponse(String userPrompt, Long projectId) {

        usageService.checkDailyTokensUsage();

        Long userId = authUtil.getCurrentUserId();

        ChatSession chatSession = createChatSessionIfNotExists(projectId, userId); //createChatSession for user if not created

        Map<String, Object> advisorParams = Map.of(  //some advisor params we are passing along with LLM call to the advisor request
                "userId", userId,
                "projectId", projectId);

        StringBuilder fullResponseBuffer = new StringBuilder();

        AtomicReference<Long> startTime = new AtomicReference<>(System.currentTimeMillis());
        AtomicReference<Long> endTime = new AtomicReference<>(0L);  //atomic reference is for get "endTime" from which thread it called
        AtomicReference<Usage> usageRef = new AtomicReference<>();
        CodeGenerationTools codeGenerationTools = new CodeGenerationTools(workspaceClient, projectId);

        return chatClient.prompt()
                .system(PromptUtils.CODE_GENERATION_SYSTEM_PROMPT) //can also Pass FileTree by just: +projectFileService.getFileTree(projectId) , but following convection
                .user(userPrompt)
                .tools(codeGenerationTools)  //our tool to readFile content
                .advisors(advisorSpec -> {
                            advisorSpec.params(advisorParams);
                            advisorSpec.advisors(fileTreeContextAdvisor); //our custom Advisor (to pass FileTree) we can do it without custom Advisor by just passing fileTree With SystemPrompt (but following best Practices)
                        }
                )
                .stream()
                .chatResponse()
                .doOnNext(response -> {
                    if (response.getResult() == null ||
                            response.getResult().getOutput() == null) {
                        return;  // skip null chunks
                    }

                    String content = response.getResult().getOutput().getText();

                    if (content != null && !content.isBlank() && endTime.get() == 0) {
                        endTime.set(System.currentTimeMillis());
                    }

                    if (response.getMetadata() != null &&
                            response.getMetadata().getUsage() != null) {
                        usageRef.set(response.getMetadata().getUsage());
                    }

                    if (content != null) {
                        fullResponseBuffer.append(content);
                    }
                    //removed below code:to remove :So when a null chunk came → boom → NPE. So now null chunks are skipped instead of crashing
//                    String content = response.getResult().getOutput().getText();  //getting Chuck
//                    if (content != null && !content.isEmpty() && endTime.get() == 0) {
//                        endTime.set(System.currentTimeMillis());
//                    }
//                    if(response.getMetadata().getUsage() != null){
//                        usageRef.set(response.getMetadata().getUsage());
//                    }
//                    fullResponseBuffer.append(content); //keep-on adding chunks to sb

                })
                .doOnComplete(() -> {  //once we get all content(we buffered in sb(fullResponseBuffer) so get full), then we Parse content & do some operations on it
                    Schedulers.boundedElastic().schedule(() -> { //by this line you are calling this method in completely diff Thread
                        Long duration = (endTime.get() - startTime.get()) / 1000;
                        try {
                            finalizeChats(userPrompt, chatSession, fullResponseBuffer.toString(), duration, usageRef.get());
                        } catch (Exception e) {
                            log.error("Finalize failed", e);
                        }
                    });    //boundedElastic -> puts a bounded(hard limit) limits on no.of Threads your application can use
                })
                .doOnError(error -> log.error("Error During Streaming for projectId: {}", projectId))
                .map(response -> {
                    if (response.getResult() == null ||
                            response.getResult().getOutput() == null) {
                        return new StreamResponse("");
                    }

                    String text = response.getResult().getOutput().getText();
                    return new StreamResponse(text != null ? text : "");
                });

    }

    //save all the Events
    private void finalizeChats(String userMessage, ChatSession chatSession, String fullText, Long duration, Usage usage) {
        Long projectId = chatSession.getId().getProjectId();

        Long userId = chatSession.getId().getUserId();
        if(usage != null){
            int totalTokens = usage.getTotalTokens();
            usageService.recordTokenUsage(userId, totalTokens);
        }

        //store the userPrompt(userMessage)
        chatMessageRepository.save(
                ChatMessage.builder()
                        .chatSession(chatSession)
                        .role(MessageRole.USER)
                        .content(userMessage)
                        .tokenUsed(usage.getPromptTokens())
                        .build()
        );

        //save the LLM chat message
        ChatMessage assistantChatMessage = ChatMessage.builder()
                .role(MessageRole.ASSISTANT)
                .chatSession(chatSession)
                .content("Assistant Message here...")
                .tokenUsed(usage.getCompletionTokens())
                .build();
        assistantChatMessage = chatMessageRepository.save(assistantChatMessage);

        List<ChatEvent> chatEventsList = llmResponseParser.parseChatEvents(fullText, assistantChatMessage);
        chatEventsList.addFirst(ChatEvent.builder()
                .type(ChatEventType.THOUGHT)
                .status(ChatEventStatus.CONFIRMED) //it already confirmed because it did thought for that much time
                .chatMessage(assistantChatMessage)
                .content("Thought for " + duration + " s")
                .sequenceOrder(0)
                .build());

        //store the file
        chatEventsList.stream()
                .filter(e -> e.getType() == ChatEventType.FILE_EDIT)
                .forEach(e -> {
//                        projectFileService.saveFile(projectId, e.getFilePath(), e.getContent() TODO: using-Kafka(done below)
                    String sagaId = UUID.randomUUID().toString();  //generate random unique id for sagaId
                    e.setSagaId(sagaId);
                    //create FileStoreRequestEvent object
                    FileStoreRequestEvent fileStoreRequestEvent = new FileStoreRequestEvent(
                            projectId, sagaId, e.getFilePath(), e.getContent(), userId
                    );
                    log.info("Storage request event Sent: {}", e.getFilePath());
                    kafkaTemplate.send("file-storage-request-event", "project-"+projectId, fileStoreRequestEvent);  //event produced from here (now consumer(workspace-service will consume this event)
                });

        //save all the Chat Events
        chatEventRepository.saveAll(chatEventsList);

    }


    //Note: ChatSession will be created only Once for everyUser for one Project
    private ChatSession createChatSessionIfNotExists(Long projectId, Long userId) {
        ChatSessionId chatSessionId = new ChatSessionId(projectId, userId);

        ChatSession chatSession = chatSessionRepository.findById(chatSessionId).orElse(null);

        if (chatSession == null) { //create new chatSession using user and project

            chatSession = ChatSession.builder()
                    .id(chatSessionId)
                    .build();
            chatSessionRepository.save(chatSession);
        }

        return chatSession;
    }


}
