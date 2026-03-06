package com.priyansu.distributed_lovable.intelligence_service.llm.tools;


import com.priyansu.distributed_lovable.intelligence_service.client.WorkspaceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@RequiredArgsConstructor
public class CodeGenerationTools {

    private final WorkspaceClient workspaceClient;

    //private final ProjectFileService projectFileService;
    private final Long projectId;

    @Tool( name = "read_files",
            description = "Read the content of files. Only input the file names present inside the FILE_TREE. DO NOT input any path which is not present under FILE_TREE."
    )
    public List<String> readFiles(
                    @ToolParam(description = "List of relative paths (e.g., ['src/App.tsx'])")
            List<String> paths){ //go Through all the file(path) oneByOne & getFile Content and pass along with Response (means LLM will decide Which file to call to read via Tool , Ask for files in Bulk to read)

        List<String> result = new ArrayList<>();

        for(String path: paths){
            String cleanPath = path.startsWith("/") ? path.substring(1) : path; //if path starts with "/" get rid of it or else as it is path (optimization fot LLM)

            log.info("Requested files from path: {}", cleanPath);

            String content = workspaceClient.getFileContent(projectId, cleanPath);

            result.add(String.format(
                    "--- START OF FILE: %s ---\n%s\n--- END OF FILE ---",
                    cleanPath, content
            ));
        }

        return result;
    }
}
