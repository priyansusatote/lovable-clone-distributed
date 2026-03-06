package com.priyansu.distributed_lovable.workspace_service.service.impl;

import com.priyansu.distributed_lovable.common_lib.exception.ResourceNotFoundException;
import com.priyansu.distributed_lovable.workspace_service.entity.Project;
import com.priyansu.distributed_lovable.workspace_service.entity.ProjectFile;
import com.priyansu.distributed_lovable.workspace_service.repository.ProjectFileRepository;
import com.priyansu.distributed_lovable.workspace_service.repository.ProjectRepository;
import com.priyansu.distributed_lovable.workspace_service.service.ProjectTemplateService;
import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectTemplateServiceImpl implements ProjectTemplateService {

    private final MinioClient minioClient;
    private final ProjectFileRepository projectFileRepository;
    private final ProjectRepository projectRepository;

    private static final String TEMPLATE_BUCKET = "starter-projects";
    private static final String TARGET_BUCKET = "projects";
    private static final String TEMPLATE_NAME = "react-vite-tailwind-daisyui-starter";


    @Override
    public void initializeProjectFromTemplate(Long projectId) {   //copy/Add Started project whenever new Project Create Request comes
        //get the project
        Project project = projectRepository.findById(projectId).orElseThrow(
                () -> new ResourceNotFoundException("Project", projectId.toString()));

        //minio code (from documentation) to duplicate file from 1 bucket and...
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(  //list all the object of Template Bucket Recursively
                    ListObjectsArgs.builder()
                            .bucket(TEMPLATE_BUCKET)
                            .prefix(TEMPLATE_NAME + "/")
                            .recursive(true)
                            .build()
            );

            List<ProjectFile> filesToSave = new ArrayList<>();  //for metadata in Postgres Db

            for (Result<Item> result : results) {
                Item item = result.get();
                String sourceKey = item.objectName();

                String cleanPath = sourceKey.replaceFirst(TEMPLATE_NAME + "/", "");
                String destKey = projectId + "/" + cleanPath; //where to copy

                minioClient.copyObject(
                        CopyObjectArgs.builder()
                                .bucket(TARGET_BUCKET)
                                .object(destKey)
                                .source(                  //src (from where to copy)
                                        CopySource.builder()
                                                .bucket(TEMPLATE_BUCKET)
                                                .object(sourceKey)
                                                .build()
                                )
                                .build()
                );

                //Create ProjectFile and save in DB
                ProjectFile pf = ProjectFile.builder()
                        .project(project)
                        .path(cleanPath)
                        .minioObjectKey(destKey)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();
                //save
                filesToSave.add(pf);
            }
            //save all the MetaData to Db
            projectFileRepository.saveAll(filesToSave);

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize project template", e);
        }
    }
}
