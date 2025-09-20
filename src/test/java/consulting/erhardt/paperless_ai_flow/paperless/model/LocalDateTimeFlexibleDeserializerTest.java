package consulting.erhardt.paperless_ai_flow.paperless.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class LocalDateTimeFlexibleDeserializerTest {
    
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }
    
    @Test
    void shouldDeserializeJsonWithDateOnlyString() throws JsonProcessingException {
        // Given: JSON with date-only string (matching real API response)
        var json = """
                {
                    "id": 589,
                    "title": "Test Document",
                    "content": "Test content",
                    "tags": [1, 2],
                    "document_type": 1,
                    "correspondent": 7,
                    "storage_path": null,
                    "created": "2025-04-07",
                    "created_date": "2025-04-07",
                    "modified": "2025-09-20T17:08:42.417119+02:00",
                    "added": "2025-04-12T22:59:09.693653+02:00",
                    "deleted_at": null,
                    "archive_serial_number": 57,
                    "original_file_name": "test.pdf",
                    "archived_file_name": "test.pdf",
                    "owner": null,
                    "user_can_change": true,
                    "is_shared_by_requester": false,
                    "notes": [],
                    "custom_fields": [],
                    "page_count": 1,
                    "mime_type": "application/pdf"
                }
                """;
        
        // When: Deserializing the JSON
        var document = objectMapper.readValue(json, PaperlessDocument.class);
        
        // Then: All LocalDateTime fields should be populated without errors
        assertThat(document).isNotNull();
        assertThat(document.getId()).isEqualTo(589L);
        assertThat(document.getTitle()).isEqualTo("Test Document");
        
        // Date-only fields should be converted to LocalDateTime at midnight
        assertThat(document.getCreated()).isEqualTo(LocalDateTime.of(2025, 4, 7, 0, 0));
        assertThat(document.getCreatedDate()).isEqualTo(LocalDateTime.of(2025, 4, 7, 0, 0));
        
        // DateTime fields should be parsed with time information
        assertThat(document.getModified()).isNotNull();
        assertThat(document.getAdded()).isNotNull();
        
        // Null datetime should remain null
        assertThat(document.getDeletedAt()).isNull();
    }
    
    @Test
    void shouldDeserializeJsonWithFullDateTimeString() throws JsonProcessingException {
        // Given: JSON with full date-time strings
        var json = """
                {
                    "id": 100,
                    "title": "Test Document with DateTime",
                    "content": "Test content",
                    "tags": [1],
                    "document_type": null,
                    "correspondent": null,
                    "storage_path": null,
                    "created": "2025-04-07T10:30:45.123456Z",
                    "created_date": "2025-04-07T10:30:45.123456Z",
                    "modified": "2025-09-20T17:08:42.417119+02:00",
                    "added": "2025-04-12T22:59:09.693653+02:00",
                    "deleted_at": null,
                    "archive_serial_number": null,
                    "original_file_name": "test.pdf",
                    "archived_file_name": "test.pdf",
                    "owner": null,
                    "user_can_change": true,
                    "is_shared_by_requester": false,
                    "notes": [],
                    "custom_fields": [],
                    "page_count": 1,
                    "mime_type": "application/pdf"
                }
                """;
        
        // When: Deserializing the JSON
        var document = objectMapper.readValue(json, PaperlessDocument.class);
        
        // Then: All LocalDateTime fields should be populated with correct time information
        assertThat(document).isNotNull();
        assertThat(document.getId()).isEqualTo(100L);
        
        // DateTime fields should preserve time information
        assertThat(document.getCreated()).isEqualTo(LocalDateTime.of(2025, 4, 7, 10, 30, 45, 123456000));
        assertThat(document.getCreatedDate()).isEqualTo(LocalDateTime.of(2025, 4, 7, 10, 30, 45, 123456000));
        assertThat(document.getModified()).isNotNull();
        assertThat(document.getAdded()).isNotNull();
    }
}
