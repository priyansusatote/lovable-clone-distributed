package com.priyansu.distributed_lovable.intelligence_service.llm.tools;

import com.priyansu.distributed_lovable.common_lib.enums.ChatEventStatus;
import com.priyansu.distributed_lovable.common_lib.enums.ChatEventType;
import com.priyansu.distributed_lovable.intelligence_service.entity.ChatEvent;
import com.priyansu.distributed_lovable.intelligence_service.entity.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component

@Slf4j
/// LLM raw text → Parser → Structured events → UI / DB / Frontend  [ parser :reads that string, extracts tags, converts them into Java objects, emits ChatEvent list]
public class LlmResponseParser {  //code from Gemini

    /**
     * Regex Breakdown:
     * Group 1: Opening Tag (<tag ...>)
     * Group 2: Tag Name (message|file|tool)
     * Group 3: Attributes part (e.g., ' path="foo"' or ' args="a,b"')
     * Group 4: Content (The stuff inside)
     * Group 5: Closing Tag (</tag>)
     */

    //helps to get tag first (message,file,tool tags)
    private static final Pattern GENERIC_TAG_PATTERN = Pattern.compile(
            "(<(message|file|tool)([^>]*)>)([\\s\\S]*?)(</\\2>)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );


    // Helper to extract specific attributes (path="..." or args="...") from Group 3
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile(
            "(phase|path|args)=\"([^\"]+)\""
    );

    //take a fullResponse message and parse it into List of ChantEvents
    public List<ChatEvent> parseChatEvents(String fullResponse, ChatMessage parentMessage) {
        List<ChatEvent> events = new ArrayList<>();
        int orderCounter = 1;

        Matcher matcher = GENERIC_TAG_PATTERN.matcher(fullResponse);

        while (matcher.find()) {
            String tagName = matcher.group(2).toLowerCase();
            String attributes = matcher.group(3);
            String content = matcher.group(4);

            // Extract attributes map
            Map<String, String> attrMap = extractAttributes(attributes);

            ChatEvent.ChatEventBuilder builder = ChatEvent.builder()
                    .status(ChatEventStatus.CONFIRMED)
                    .chatMessage(parentMessage)
                    .content(content) // This is your Markdown content
                    .sequenceOrder(orderCounter++);

            switch (tagName) {
                case "message" -> {
                    String phase = attrMap.get("phase");
                    if ("tool".equalsIgnoreCase(phase)) {
                        // Treat <message phase="tool"> as a Tool Event (Spinner UI)
                        builder.type(ChatEventType.TOOL_LOG);
                        builder.metadata(attrMap.get("metadata"));
                    } else {
                        // Normal text message
                        builder.type(ChatEventType.MESSAGE);
                    }
                }
                case "file" -> {
                    String path = attrMap.get("path");
                    if (content == null || content.isBlank()) {
                        log.error("Skipping empty file for {}", path);
                        continue;
                    }
                    builder.type(ChatEventType.FILE_EDIT);
                    builder.status(ChatEventStatus.PENDING);  //pending state by default (because it needs file-save event confirmation from minIo(workspace-service)
                    builder.filePath(attrMap.get("path"));
                }
                default -> log.warn("Unknown tag detected: {}", tagName);
            }
            events.add(builder.build());
        }

        return events;
    }

    private Map<String, String> extractAttributes(String attributeString) {
        Map<String, String> attributes = new HashMap<>();
        if (attributeString == null) return attributes;

        Matcher matcher = ATTRIBUTE_PATTERN.matcher(attributeString);
        while (matcher.find()) {
            attributes.put(matcher.group(1), matcher.group(2));
        }
        return attributes;
    }
}
