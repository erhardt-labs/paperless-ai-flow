package consulting.erhardt.paperless_ai_flow.paperless.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Paperless-ngx document model based on API v9
 */
@Value
@Builder
@Jacksonized
public class PaperlessDocument {
    
    Long id;
    
    String title;
    
    String content;
    
    @JsonProperty("tags")
    List<Long> tagIds;
    
    @JsonProperty("document_type")
    Long documentTypeId;
    
    @JsonProperty("correspondent")
    Long correspondentId;
    
    @JsonProperty("created")
    @JsonDeserialize(using = LocalDateTimeFlexibleDeserializer.class)
    LocalDateTime created;
    
    @JsonProperty("created_date")
    @JsonDeserialize(using = LocalDateTimeFlexibleDeserializer.class)
    LocalDateTime createdDate;
    
    @JsonProperty("modified")
    @JsonDeserialize(using = LocalDateTimeFlexibleDeserializer.class)
    LocalDateTime modified;
    
    @JsonProperty("added")
    @JsonDeserialize(using = LocalDateTimeFlexibleDeserializer.class)
    LocalDateTime added;
    
    @JsonProperty("deleted_at")
    @JsonDeserialize(using = LocalDateTimeFlexibleDeserializer.class)
    LocalDateTime deletedAt;
    
    @JsonProperty("storage_path")
    String storagePath;
    
    @JsonProperty("archive_serial_number")
    Long archiveSerialNumber;
    
    @JsonProperty("original_file_name")
    String originalFileName;
    
    @JsonProperty("archived_file_name")
    String archivedFileName;
    
    @JsonProperty("owner")
    Long ownerId;
    
    @JsonProperty("user_can_change")
    Boolean userCanChange;
    
    @JsonProperty("is_shared_by_requester")
    Boolean isSharedByRequester;
    
    @JsonProperty("notes")
    List<String> notes;
    
    @JsonProperty("custom_fields")
    List<CustomField> customFields;
    
    @JsonProperty("page_count")
    Integer pageCount;
    
    @JsonProperty("mime_type")
    String mimeType;
    
    @Value
    @Builder
    @Jacksonized
    public static class CustomField {
        Long field;
        String value;
    }
}
