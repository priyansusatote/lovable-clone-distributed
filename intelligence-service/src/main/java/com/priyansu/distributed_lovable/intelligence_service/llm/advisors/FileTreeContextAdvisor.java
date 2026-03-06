package com.priyansu.distributed_lovable.intelligence_service.llm.advisors;


import com.priyansu.distributed_lovable.common_lib.dto.FileNode;
import com.priyansu.distributed_lovable.intelligence_service.client.WorkspaceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class FileTreeContextAdvisor implements StreamAdvisor {

    private final WorkspaceClient workspaceClient;

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain streamAdvisorChain) {

        Map<String, Object> context = request.context(); //will give context that we passed while Generating ChatClient (metadata for advisors / tracing etc.) (commonly used to pass metadata across advisors/filters (like userId, sessionId,...)
        Long projectId = Long.parseLong(context.getOrDefault("projectId", 0).toString());

        ChatClientRequest augmentedChatClientRequest = augmentRequestWithFileTree(request, projectId);  //will have FileTree

        return streamAdvisorChain.nextStream(augmentedChatClientRequest);
    }

    private ChatClientRequest augmentRequestWithFileTree(ChatClientRequest request, Long projectId) {  //take the prompt(from request) and Add the FileTree at End of the Prompt
        //Note:if u have common Prefix(like System Prompt in Every request) in Prompt for that Prefix Cashing hit by LLM (it helps to reduce cost) , and adding fileTree to end so easier to avoid
        //so we are ensuring System-Prompt should be in Beginning and file tree should not be, code is bellow

        //taking-out 2-part from ChatClientRequest request (userPrompt, SystemPrompt)
        List<Message> incomingMessages = request.prompt().getInstructions();

        //SystemMessage
        Message systemMessage = incomingMessages.stream()
                .filter(m -> m.getMessageType() == MessageType.SYSTEM) //java8-Filter
                .findFirst()
                .orElse(null);

        List<Message> userMessages = incomingMessages.stream()
                .filter(m -> m.getMessageType() != MessageType.SYSTEM)
                .toList();

        List<Message> allMessages = new ArrayList<>(); //Empty

        // Add original system message
        if (systemMessage != null) {
            allMessages.add(systemMessage);  //first Add SystemMessage (this make sure System-Prompt always first written (for Caching purpose))
        }


        List<FileNode> fileTree = workspaceClient.getFileTree(projectId).files();
        String fileTreeContext = "\n\n ---- FILE_TREE ----\n" + fileTree.toString();  //to divide in another section (easier for LLM)

        // (add file-tree)
        allMessages.add(new SystemMessage(fileTreeContext));

        allMessages.addAll(userMessages);  //user-Prompt added after System-Prompt added above in "allMessages"

        return request  //new ChatClientRequest
                .mutate()
                .prompt(new Prompt(allMessages, request.prompt().getOptions()))  //all messages = (System + User) Prompt
                .build();
    }

    @Override
    public String getName() {
        return "FileTreeContextAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
