package com.innovate;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.*;

/**
 * For implement this task focus on clear code, and make this solution as simple readable as possible
 * Don't worry about performance, concurrency, etc
 * You can use in Memory collection for sore data
 * <p>
 * Please, don't change class name, and signature for methods save, search, findById
 * Implementations should be in a single class
 * This class could be auto tested
 */
public class DocumentManager {

    private final StorageDB storage;

    public DocumentManager(StorageDB storage) {
        this.storage = storage;
    }

    /**
     * Implementation of this method should upsert the document to your storage
     * And generate unique id if it does not exist, don't change [created] field
     *
     * @param document - document content and author data
     * @return saved document
     */
    public Document save(Document document) {
        if (document == null) throw new IllegalArgumentException("Document cannot be null");

        Document existingDocument = storage.getDocumentById(document.getId());
        if (existingDocument != null) {
            document.setCreated(existingDocument.getCreated());
        }

        return storage.saveDocument(document);
    }

    /**
     * Implementation this method should find documents which match with request
     *
     * @param request - search request, each field could be null
     * @return list matched documents
     */
    public List<Document> search(SearchRequest request) {
        if (request == null) throw new IllegalArgumentException("SearchRequest must not be null");

        return storage.searchDocuments(request);
    }

    /**
     * Implementation this method should find document by id
     *
     * @param id - document id
     * @return optional document
     */
    public Optional<Document> findById(String id) {
        return Optional.ofNullable(storage.getDocumentById(id));
    }

    @Data
    @Builder
    public static class SearchRequest {
        private List<String> titlePrefixes;
        private List<String> containsContents;
        private List<String> authorIds;
        private Instant createdFrom;
        private Instant createdTo;
    }

    @Data
    @Builder
    public static class Document {
        private String id;
        private String title;
        private String content;
        private Author author;
        private Instant created;
    }

    @Data
    @Builder
    public static class Author {
        private String id;
        private String name;
    }

    @Data
    public static class StorageDB {

        private final Map<String, Document> storageDB;

        public Document saveDocument(Document document) {
            String id = document.getId();
            if (id == null || id.trim().isEmpty()) {
                id = UUID.randomUUID().toString();
                document.setId(id);
            }
            storageDB.put(id, document);

            return document;
        }

        public List<Document> searchDocuments(SearchRequest request) {
            List<Document> documents = new ArrayList<>();

            for (Map.Entry<String, Document> entry : storageDB.entrySet()) {
                Document document = entry.getValue();
                if (isValidDocument(request, document)) {
                    documents.add(document);
                }
            }

            return documents;
        }

        private boolean isValidDocument(SearchRequest request, Document document) {
            if (request.getCreatedFrom() != null && document.getCreated().isBefore(request.getCreatedFrom())) {
                return false;
            }
            if (request.getCreatedTo() != null && document.getCreated().isAfter(request.getCreatedTo())) {
                return false;
            }

            if (request.getTitlePrefixes() != null && !request.getTitlePrefixes().isEmpty()
                    && document.getTitle() != null
                    && request.getTitlePrefixes().stream().noneMatch(prefix -> document.getTitle().startsWith(prefix))) {
                return false;
            }

            if (request.getContainsContents() != null && !request.getContainsContents().isEmpty()
                    && document.getContent() != null
                    && request.getContainsContents().stream().noneMatch(content -> document.getContent().contains(content))) {
                return false;
            }

            if (request.getAuthorIds() != null && !request.getAuthorIds().isEmpty()
                    && document.getAuthor() != null
                    && !request.getAuthorIds().contains(document.getAuthor().getId())) {
                return false;
            }

            return true;
        }

        public Document getDocumentById(String id) {
            return storageDB.get(id);
        }
    }
}
