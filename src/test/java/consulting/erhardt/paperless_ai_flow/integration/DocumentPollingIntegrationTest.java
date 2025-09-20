package consulting.erhardt.paperless_ai_flow.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import consulting.erhardt.paperless_ai_flow.paperless.model.PaperlessApiResponse;
import consulting.erhardt.paperless_ai_flow.paperless.model.PaperlessDocument;
import consulting.erhardt.paperless_ai_flow.paperless.model.PaperlessTag;
import consulting.erhardt.paperless_ai_flow.service.DocumentPollingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for document polling using WireMock to simulate Paperless-ngx API
 */
@SpringBootTest(properties = {
        "paperless.api.token=test-token",
        "paperless.pipelines[0].name=test_pipeline",
        "paperless.pipelines[0].selector.required-tags[0]=inbox",
        "paperless.pipelines[0].selector.required-tags[1]=pipeline-auto",
        "paperless.pipelines[0].polling.interval=PT10S",
        "paperless.pipelines[0].polling.enabled=true",
        "paperless.pipelines[0].ocr.model=openai/gpt-4o",
        "paperless.pipelines[0].ocr.prompt=Extract text from this PDF"
})
class DocumentPollingIntegrationTest {
    
    private WireMockServer wireMockServer;
    
    @Autowired
    private DocumentPollingService pollingService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Use a fixed port to avoid port conflicts between tests
        registry.add("paperless.api.base-url", () -> "http://localhost:8089");
    }
    
    @BeforeEach
    void setUp() {
        // Start WireMock server before each test with fixed port
        wireMockServer = new WireMockServer(WireMockConfiguration.options().port(8089));
        wireMockServer.start();
        wireMockServer.resetAll();
    }
    
    @AfterEach
    void tearDown() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }
    
    @Test
    void shouldPollDocumentsWithMatchingTags() throws Exception {
        // Given: Mock tags endpoint
        var inboxTag = PaperlessTag.builder()
                .id(1L)
                .name("inbox")
                .slug("inbox")
                .build();
        
        var pipelineAutoTag = PaperlessTag.builder()
                .id(2L)
                .name("pipeline-auto")
                .slug("pipeline-auto")
                .build();
        
        var otherTag = PaperlessTag.builder()
                .id(3L)
                .name("other")
                .slug("other")
                .build();
        
        var tagsResponse = PaperlessApiResponse.<PaperlessTag>builder()
                .count(3)
                .results(List.of(inboxTag, pipelineAutoTag, otherTag))
                .build();
        
        wireMockServer.stubFor(get(urlEqualTo("/api/tags/"))
                .withHeader("Authorization", equalTo("Token test-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(tagsResponse))));
        
        // Given: Mock documents endpoint with matching documents using date-only strings to match real API
        var documentsJsonResponse = """
                {
                    "count": 2,
                    "next": null,
                    "previous": null,
                    "results": [
                        {
                            "id": 100,
                            "title": "Test Document 1",
                            "content": "Sample content 1",
                            "tags": [1, 2],
                            "document_type": null,
                            "correspondent": null,
                            "storage_path": null,
                            "created": "2025-04-07",
                            "created_date": "2025-04-07",
                            "modified": "2025-09-20T17:08:42.417119+02:00",
                            "added": "2025-04-12T22:59:09.693653+02:00",
                            "deleted_at": null,
                            "archive_serial_number": 57,
                            "original_file_name": "test1.pdf",
                            "archived_file_name": "test1.pdf",
                            "owner": null,
                            "user_can_change": true,
                            "is_shared_by_requester": false,
                            "notes": [],
                            "custom_fields": [],
                            "page_count": 1,
                            "mime_type": "application/pdf"
                        },
                        {
                            "id": 101,
                            "title": "Test Document 2",
                            "content": "Sample content 2",
                            "tags": [1, 2],
                            "document_type": null,
                            "correspondent": null,
                            "storage_path": null,
                            "created": "2025-04-08",
                            "created_date": "2025-04-08",
                            "modified": "2025-09-20T18:08:42.417119+02:00",
                            "added": "2025-04-12T23:59:09.693653+02:00",
                            "deleted_at": null,
                            "archive_serial_number": 58,
                            "original_file_name": "test2.pdf",
                            "archived_file_name": "test2.pdf",
                            "owner": null,
                            "user_can_change": true,
                            "is_shared_by_requester": false,
                            "notes": [],
                            "custom_fields": [],
                            "page_count": 1,
                            "mime_type": "application/pdf"
                        }
                    ]
                }
                """;
        
        wireMockServer.stubFor(get(urlPathEqualTo("/api/documents/"))
                .withQueryParam("tags__id__all", equalTo("1,2"))
                .withQueryParam("ordering", equalTo("-added"))
                .withHeader("Authorization", equalTo("Token test-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(documentsJsonResponse)));
        
        // When: Poll documents using test pipeline configuration
        var testPipeline = consulting.erhardt.paperless_ai_flow.config.PipelineConfiguration.PipelineDefinition.builder()
                .name("test_pipeline")
                .selector(consulting.erhardt.paperless_ai_flow.config.PipelineConfiguration.SelectorConfiguration.builder()
                        .requiredTags(List.of("inbox", "pipeline-auto"))
                        .build())
                .ocr(consulting.erhardt.paperless_ai_flow.config.PipelineConfiguration.OcrConfiguration.builder()
                        .model("openai/gpt-4o")
                        .prompt("Extract text from this PDF")
                        .build())
                .polling(consulting.erhardt.paperless_ai_flow.config.PipelineConfiguration.PollingConfiguration.builder()
                        .enabled(true)
                        .interval(java.time.Duration.ofSeconds(10))
                        .build())
                .build();
        
        var polledDocuments = pollingService.pollDocuments(testPipeline);
        
        // Then: Verify documents were retrieved
        assertThat(polledDocuments)
                .hasSize(2)
                .extracting(PaperlessDocument::getId)
                .containsExactly(100L, 101L);
        
        assertThat(polledDocuments)
                .extracting(PaperlessDocument::getTitle)
                .containsExactly("Test Document 1", "Test Document 2");
        
        // Verify API calls were made
        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/api/tags/")));
        wireMockServer.verify(1, getRequestedFor(urlPathEqualTo("/api/documents/"))
                .withQueryParam("tags__id__all", equalTo("1,2")));
    }
    
    @Test
    void shouldReturnEmptyListWhenNoMatchingTags() throws Exception {
        // Given: Mock tags endpoint with no matching tags
        var otherTag = PaperlessTag.builder()
                .id(3L)
                .name("other")
                .slug("other")
                .build();
        
        var tagsResponse = PaperlessApiResponse.<PaperlessTag>builder()
                .count(1)
                .results(List.of(otherTag))
                .build();
        
        wireMockServer.stubFor(get(urlEqualTo("/api/tags/"))
                .withHeader("Authorization", equalTo("Token test-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(tagsResponse))));
        
        // When: Poll documents using test pipeline configuration
        var testPipeline = consulting.erhardt.paperless_ai_flow.config.PipelineConfiguration.PipelineDefinition.builder()
                .name("test_pipeline")
                .selector(consulting.erhardt.paperless_ai_flow.config.PipelineConfiguration.SelectorConfiguration.builder()
                        .requiredTags(List.of("inbox", "pipeline-auto"))
                        .build())
                .ocr(consulting.erhardt.paperless_ai_flow.config.PipelineConfiguration.OcrConfiguration.builder()
                        .model("openai/gpt-4o")
                        .prompt("Extract text from this PDF")
                        .build())
                .polling(consulting.erhardt.paperless_ai_flow.config.PipelineConfiguration.PollingConfiguration.builder()
                        .enabled(true)
                        .interval(java.time.Duration.ofSeconds(10))
                        .build())
                .build();
        
        var polledDocuments = pollingService.pollDocuments(testPipeline);
        
        // Then: Verify empty list is returned
        assertThat(polledDocuments).isEmpty();
        
        // Verify tags API was called but not documents API
        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/api/tags/")));
        wireMockServer.verify(0, getRequestedFor(urlPathEqualTo("/api/documents/")));
    }
    
    @Test
    void shouldHandleDateOnlyAndDateTimeStrings() throws Exception {
        // Given: Mock tags endpoint
        var inboxTag = PaperlessTag.builder()
                .id(1L)
                .name("inbox")
                .slug("inbox")
                .build();
        
        var tagsResponse = PaperlessApiResponse.<PaperlessTag>builder()
                .count(1)
                .results(List.of(inboxTag))
                .build();
        
        wireMockServer.stubFor(get(urlEqualTo("/api/tags/"))
                .withHeader("Authorization", equalTo("Token test-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(tagsResponse))));
        
        // Given: Mock documents endpoint with mixed date formats (date-only for "created", datetime for "modified")
        var mixedDateFormatJsonResponse = """
                {
                    "count": 1,
                    "next": null,
                    "previous": null,
                    "results": [
                        {
                            "id": 589,
                            "title": "Mixed Date Format Document",
                            "content": "Test content with mixed date formats",
                            "tags": [1],
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
                    ]
                }
                """;
        
        wireMockServer.stubFor(get(urlPathEqualTo("/api/documents/"))
                .withQueryParam("tags__id__all", equalTo("1"))
                .withQueryParam("ordering", equalTo("-added"))
                .withHeader("Authorization", equalTo("Token test-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mixedDateFormatJsonResponse)));
        
        // When: Poll documents using test pipeline configuration
        var testPipeline = consulting.erhardt.paperless_ai_flow.config.PipelineConfiguration.PipelineDefinition.builder()
                .name("test_pipeline")
                .selector(consulting.erhardt.paperless_ai_flow.config.PipelineConfiguration.SelectorConfiguration.builder()
                        .requiredTags(List.of("inbox"))
                        .build())
                .ocr(consulting.erhardt.paperless_ai_flow.config.PipelineConfiguration.OcrConfiguration.builder()
                        .model("openai/gpt-4o")
                        .prompt("Extract text from this PDF")
                        .build())
                .polling(consulting.erhardt.paperless_ai_flow.config.PipelineConfiguration.PollingConfiguration.builder()
                        .enabled(true)
                        .interval(java.time.Duration.ofSeconds(10))
                        .build())
                .build();
        
        var polledDocuments = pollingService.pollDocuments(testPipeline);
        
        // Then: Verify documents were retrieved and date fields are properly parsed
        assertThat(polledDocuments)
                .hasSize(1)
                .extracting(PaperlessDocument::getId)
                .containsExactly(589L);
        
        var document = polledDocuments.get(0);
        assertThat(document.getCreated()).isNotNull(); // date-only should be converted to LocalDateTime at midnight
        assertThat(document.getModified()).isNotNull(); // datetime string should be parsed normally
        assertThat(document.getAdded()).isNotNull(); // datetime string should be parsed normally
        
        // Verify API calls were made
        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/api/tags/")));
        wireMockServer.verify(1, getRequestedFor(urlPathEqualTo("/api/documents/"))
                .withQueryParam("tags__id__all", equalTo("1")));
    }
    
    @Test
    void shouldHandleApiErrorsGracefully() throws Exception {
        // Given: Mock tags endpoint returns error
        wireMockServer.stubFor(get(urlEqualTo("/api/tags/"))
                .withHeader("Authorization", equalTo("Token test-token"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));
        
        // When: Poll documents using test pipeline configuration
        var testPipeline = consulting.erhardt.paperless_ai_flow.config.PipelineConfiguration.PipelineDefinition.builder()
                .name("test_pipeline")
                .selector(consulting.erhardt.paperless_ai_flow.config.PipelineConfiguration.SelectorConfiguration.builder()
                        .requiredTags(List.of("inbox", "pipeline-auto"))
                        .build())
                .ocr(consulting.erhardt.paperless_ai_flow.config.PipelineConfiguration.OcrConfiguration.builder()
                        .model("openai/gpt-4o")
                        .prompt("Extract text from this PDF")
                        .build())
                .polling(consulting.erhardt.paperless_ai_flow.config.PipelineConfiguration.PollingConfiguration.builder()
                        .enabled(true)
                        .interval(java.time.Duration.ofSeconds(10))
                        .build())
                .build();
        
        var polledDocuments = pollingService.pollDocuments(testPipeline);
        
        // Then: Verify error is handled gracefully and empty list is returned
        assertThat(polledDocuments).isEmpty();
        
        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/api/tags/")));
    }
}
