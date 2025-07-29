/*
 * Copyright (c) 2010-2025 Progress Software Corporation and/or its subsidiaries or affiliates. All Rights Reserved.
 */
package com.example.mcpserver.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class MarkLogicDocsService {

    private static final Logger logger = LoggerFactory.getLogger(MarkLogicDocsService.class);
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final DocumentSplitter documentSplitter;

    public MarkLogicDocsService() {
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        this.embeddingStore = new InMemoryEmbeddingStore();
        this.documentSplitter = DocumentSplitters.recursive(1500, 200);
    }

    @PostConstruct
    public void initialize() {
        loadZip("flux-docs.zip");
        loadZip("ml-gradle-docs.zip");
    }

    public List<EmbeddingMatch<TextSegment>> search(String query, int maxResults) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .maxResults(maxResults)
                .queryEmbedding(queryEmbedding)
                .build();

        EmbeddingSearchResult result = embeddingStore.search(request);
        return result.matches();
    }

    private void loadZip(String filename) {
        try {
            ClassPathResource resource = new ClassPathResource(filename);

            try (InputStream inputStream = resource.getInputStream();
                 ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {

                ZipEntry entry;
                while ((entry = zipInputStream.getNextEntry()) != null) {
                    if (!entry.isDirectory()) {
                        String fileName = entry.getName();
                        String content = readEntryContent(zipInputStream);

                        logger.info("Processing zip entry: {} (size: {} bytes)",
                                fileName, content.length());

                        // TODO: Add your processing logic here
                        processEntry(fileName, content);
                    }
                    zipInputStream.closeEntry();
                }
            }

            logger.info("Finished loading flux-docs.zip");

        } catch (IOException e) {
            logger.error("Error loading flux-docs.zip", e);
            throw new RuntimeException("Failed to load flux documentation", e);
        }
    }

    private String readEntryContent(ZipInputStream zipInputStream) throws IOException {
        StringBuilder content = new StringBuilder();
        byte[] buffer = new byte[1024];
        int length;

        while ((length = zipInputStream.read(buffer)) > 0) {
            content.append(new String(buffer, 0, length, StandardCharsets.UTF_8));
        }

        return content.toString();
    }

    private void processEntry(String fileName, String content) {
        Document originalDocument = Document.from(content, Metadata.from("fileName", fileName));
        List<TextSegment> chunks = documentSplitter.split(originalDocument);

        logger.debug("Split {} into {} chunks", fileName, chunks.size());

        for (int i = 0; i < chunks.size(); i++) {
            TextSegment chunk = chunks.get(i);

            Metadata chunkMetadata = Metadata.from("fileName", fileName)
                    .put("chunkIndex", String.valueOf(i))
                    .put("totalChunks", String.valueOf(chunks.size()));

            TextSegment segmentWithMetadata = TextSegment.from(chunk.text(), chunkMetadata);

            // Create embedding and store
            Embedding embedding = embeddingModel.embed(segmentWithMetadata).content();
            embeddingStore.add(embedding, segmentWithMetadata);
        }
    }
}