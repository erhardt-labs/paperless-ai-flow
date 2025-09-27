package consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.PaperlessNgxApiClient;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.configs.TestPaperlessNgxHttpClientConfig;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Correspondent;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.CustomField;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Document;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Tag;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities.DocumentPatchRequest;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities.DocumentResponse;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.mappers.DocumentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = TestPaperlessNgxHttpClientConfig.class)
@TestPropertySource(properties = {
    "paperless.api.base-url=http://localhost:8089",
    "paperless.api.token=test-token"
})
class DocumentServiceWireMockTest {

  @RegisterExtension
  static WireMockExtension wireMockExtension = WireMockExtension.newInstance()
      .options(wireMockConfig().port(8089))
      .build();

  @Autowired
  private PaperlessNgxApiClient apiClient;

  @Mock
  private DocumentMapper documentMapper;

  @Mock
  private CorrespondentService correspondentService;

  @Mock
  private CustomFieldsService customFieldsService;

  @Mock
  private TagService tagService;

  private DocumentService documentService;
  private ObjectMapper objectMapper;
  private static final String PATCHED_DOCUMENT_SCHEMA_PATH = "/schemas/PatchedDocumentRequest.json";

  @BeforeEach
  void setUp() throws IOException {
    documentService = new DocumentService(apiClient, documentMapper, correspondentService, customFieldsService, tagService);
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

    // Load and parse the JSON schema for validation
    var schemaInputStream = getClass().getResourceAsStream(PATCHED_DOCUMENT_SCHEMA_PATH);
    assertThat(schemaInputStream).isNotNull();
  }

  @Test
  void patchById_shouldSendValidSchemaCompliantRequest() throws Exception {
    // Given
    var documentId = 123;
    var document = Document.builder()
        .id(documentId)
        .title("Updated Test Document")
        .content("Updated content for testing")
        .createdDate(LocalDate.of(2024, 1, 15))
        .correspondent(Correspondent.builder()
            .id(1)
            .name("Test Correspondent")
            .slug("test-correspondent")
            .build())
        .tags(List.of(
            Tag.builder().id(1).name("Important").slug("important").build(),
            Tag.builder().id(2).name("Personal").slug("personal").build()
        ))
        .customFields(List.of(
            CustomField.builder()
                .id(1)
                .name("Priority")
                .dataType("string")
                .value("High")
                .build()
        ))
        .build();

    var expectedResponse = """
        {
          "id": 123,
          "title": "Updated Test Document",
          "content": "Updated content for testing",
          "created": "2024-01-15",
          "correspondent": 1,
          "tags": [1, 2],
          "custom_fields": [
            {
              "field": 1,
              "value": "High"
            }
          ]
        }
        """;

    // Setup mapper mock
    var patchRequest = DocumentPatchRequest.builder()
        .title("Updated Test Document")
        .content("Updated content for testing")
        .created(LocalDate.of(2024, 1, 15))
        .correspondentId(1)
        .tagIds(List.of(1, 2))
        .customFields(Map.of(1, "High"))
        .removeInboxTags(false)
        .build();

    when(documentMapper.toPatchRequest(document)).thenReturn(patchRequest);

    // Setup service mocks
    when(correspondentService.getById(1)).thenReturn(Mono.just(document.getCorrespondent()));
    when(customFieldsService.getById(1)).thenReturn(Mono.just(document.getCustomFields().get(0)));
    when(tagService.getById(1)).thenReturn(Mono.just(document.getTags().get(0)));
    when(tagService.getById(2)).thenReturn(Mono.just(document.getTags().get(1)));

    var documentResponse = DocumentResponse.builder()
        .id(documentId)
        .title("Updated Test Document")
        .content("Updated content for testing")
        .createdDate(LocalDate.of(2024, 1, 15))
        .correspondentId(1)
        .tagIds(List.of(1, 2))
        .customFields(List.of(DocumentResponse.CustomField.builder()
            .id(1)
            .value("High")
            .build()))
        .build();

    when(documentMapper.toDto(any(DocumentResponse.class), any(), any(), any())).thenReturn(document);

    // Set up WireMock to validate request schema compliance
    wireMockExtension.stubFor(patch(urlEqualTo("/api/documents/123/"))
        .withRequestBody(matchingJsonSchema(loadSchema()))
        .withHeader("Authorization", equalTo("Token test-token"))
        .withHeader("Content-Type", containing("application/json"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(expectedResponse)));

    // When & Then
    StepVerifier.create(documentService.patchById(documentId, document))
        .expectNextMatches(updatedDoc ->
            updatedDoc.getId().equals(documentId) &&
            "Updated Test Document".equals(updatedDoc.getTitle()) &&
            "Updated content for testing".equals(updatedDoc.getContent()) &&
            LocalDate.of(2024, 1, 15).equals(updatedDoc.getCreatedDate()))
        .verifyComplete();

    // Verify the request was made correctly
    wireMockExtension.verify(exactly(1), patchRequestedFor(urlEqualTo("/api/documents/123/")));
  }

  @Test
  void patchById_shouldMakeCorrectRestRequest() throws Exception {
    // Given
    var documentId = 456;
    var document = Document.builder()
        .id(documentId)
        .title("Another Test Document")
        .content("Test content")
        .createdDate(LocalDate.of(2024, 2, 20))
        .build();

    var expectedResponse = """
        {
          "id": 456,
          "title": "Another Test Document",
          "content": "Test content",
          "created": "2024-02-20",
          "correspondent": null,
          "tags": [],
          "custom_fields": []
        }
        """;

    // Setup mapper mock
    var patchRequest = DocumentPatchRequest.builder()
        .title("Another Test Document")
        .content("Test content")
        .created(LocalDate.of(2024, 2, 20))
        .removeInboxTags(false)
        .build();

    when(documentMapper.toPatchRequest(document)).thenReturn(patchRequest);

    var documentResponse = DocumentResponse.builder()
        .id(documentId)
        .title("Another Test Document")
        .content("Test content")
        .createdDate(LocalDate.of(2024, 2, 20))
        .build();

    when(documentMapper.toDto(any(DocumentResponse.class), any(), any(), any())).thenReturn(document);

    // Set up WireMock with custom request matcher
    wireMockExtension.stubFor(patch(urlEqualTo("/api/documents/456/"))
        .withRequestBody(matchingJsonPath("$.title", equalTo("Another Test Document")))
        .withRequestBody(matchingJsonPath("$.content", equalTo("Test content")))
        .withRequestBody(matchingJsonPath("$.created", equalTo("2024-02-20")))
        .withRequestBody(matchingJsonPath("$.remove_inbox_tags", equalTo("false")))
        .withHeader("Authorization", equalTo("Token test-token"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(expectedResponse)));

    // When & Then
    StepVerifier.create(documentService.patchById(documentId, document))
        .expectNextMatches(updatedDoc ->
            updatedDoc.getId().equals(documentId) &&
            "Another Test Document".equals(updatedDoc.getTitle()))
        .verifyComplete();

    // Verify the exact request structure
    wireMockExtension.verify(patchRequestedFor(urlEqualTo("/api/documents/456/"))
        .withRequestBody(matchingJsonPath("$.title"))
        .withRequestBody(matchingJsonPath("$.content"))
        .withRequestBody(matchingJsonPath("$.created"))
        .withRequestBody(matchingJsonPath("$.remove_inbox_tags"))
        .withHeader("Authorization", equalTo("Token test-token")));
  }

  @Test
  void patchById_shouldHandleComplexCustomFields() throws Exception {
    // Given
    var documentId = 789;
    var customFields = List.of(
        CustomField.builder()
            .id(1)
            .name("StringField")
            .dataType("string")
            .value("String Value")
            .build(),
        CustomField.builder()
            .id(2)
            .name("IntegerField")
            .dataType("integer")
            .value("42")
            .build(),
        CustomField.builder()
            .id(3)
            .name("NumberField")
            .dataType("number")
            .value("3.14")
            .build()
    );

    var document = Document.builder()
        .id(documentId)
        .title("Complex Custom Fields Test")
        .customFields(customFields)
        .build();

    var expectedResponse = """
        {
          "id": 789,
          "title": "Complex Custom Fields Test",
          "custom_fields": [
            {"field": 1, "value": "String Value"},
            {"field": 2, "value": "42"},
            {"field": 3, "value": "3.14"}
          ]
        }
        """;

    // Setup mapper mock
    var patchRequest = DocumentPatchRequest.builder()
        .title("Complex Custom Fields Test")
        .customFields(Map.of(1, "String Value", 2, "42", 3, "3.14"))
        .removeInboxTags(false)
        .build();

    when(documentMapper.toPatchRequest(document)).thenReturn(patchRequest);

    // Setup service mocks
    when(customFieldsService.getById(1)).thenReturn(Mono.just(customFields.get(0)));
    when(customFieldsService.getById(2)).thenReturn(Mono.just(customFields.get(1)));
    when(customFieldsService.getById(3)).thenReturn(Mono.just(customFields.get(2)));

    var documentResponse = DocumentResponse.builder()
        .id(documentId)
        .title("Complex Custom Fields Test")
        .customFields(List.of(
            DocumentResponse.CustomField.builder().id(1).value("String Value").build(),
            DocumentResponse.CustomField.builder().id(2).value("42").build(),
            DocumentResponse.CustomField.builder().id(3).value("3.14").build()
        ))
        .build();

    when(documentMapper.toDto(any(DocumentResponse.class), any(), any(), any())).thenReturn(document);

    // Verify custom fields are serialized as array format - use individual matchers since order may vary
    wireMockExtension.stubFor(patch(urlEqualTo("/api/documents/789/"))
        .withRequestBody(matchingJsonPath("$.custom_fields[?(@.field == 1)].value", equalTo("String Value")))
        .withRequestBody(matchingJsonPath("$.custom_fields[?(@.field == 2)].value", equalTo("42")))
        .withRequestBody(matchingJsonPath("$.custom_fields[?(@.field == 3)].value", equalTo("3.14")))
        .withRequestBody(matchingJsonPath("$.custom_fields.length()", equalTo("3")))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(expectedResponse)));

    // When & Then
    StepVerifier.create(documentService.patchById(documentId, document))
        .expectNextMatches(updatedDoc ->
            updatedDoc.getId().equals(documentId) &&
            updatedDoc.getCustomFields().size() == 3)
        .verifyComplete();
  }

  @Test
  void patchById_shouldHandleEmptyOptionalFields() throws Exception {
    // Given
    var documentId = 999;
    var document = Document.builder()
        .id(documentId)
        .title("Minimal Document")
        .build();

    var expectedResponse = """
        {
          "id": 999,
          "title": "Minimal Document",
          "correspondent": null,
          "tags": [],
          "custom_fields": []
        }
        """;

    // Setup mapper mock
    var patchRequest = DocumentPatchRequest.builder()
        .title("Minimal Document")
        .removeInboxTags(false)
        .build();

    when(documentMapper.toPatchRequest(document)).thenReturn(patchRequest);

    var documentResponse = DocumentResponse.builder()
        .id(documentId)
        .title("Minimal Document")
        .build();

    when(documentMapper.toDto(any(DocumentResponse.class), any(), any(), any())).thenReturn(document);

    // Verify null/empty fields are handled correctly
    wireMockExtension.stubFor(patch(urlEqualTo("/api/documents/999/"))
        .withRequestBody(matchingJsonPath("$.title", equalTo("Minimal Document")))
        .withRequestBody(matchingJsonPath("$.remove_inbox_tags", equalTo("false")))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(expectedResponse)));

    // When & Then
    StepVerifier.create(documentService.patchById(documentId, document))
        .expectNextMatches(updatedDoc ->
            updatedDoc.getId().equals(documentId) &&
            "Minimal Document".equals(updatedDoc.getTitle()))
        .verifyComplete();
  }

  private String loadSchema() throws IOException {
    try (var inputStream = getClass().getResourceAsStream(PATCHED_DOCUMENT_SCHEMA_PATH)) {
      assertThat(inputStream).isNotNull();
      return new String(inputStream.readAllBytes());
    }
  }
}
