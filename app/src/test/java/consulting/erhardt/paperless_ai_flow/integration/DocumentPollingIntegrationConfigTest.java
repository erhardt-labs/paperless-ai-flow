package consulting.erhardt.paperless_ai_flow.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import consulting.erhardt.paperless_ai_flow.configs.PipelineConfiguration;
import consulting.erhardt.paperless_ai_flow.configs.PipelineConfiguration.PatchConfiguration;
import consulting.erhardt.paperless_ai_flow.configs.PipelineConfiguration.PatchType;
import consulting.erhardt.paperless_ai_flow.configs.PipelineConfiguration.PipelineDefinition;
import consulting.erhardt.paperless_ai_flow.configs.PipelineConfiguration.SelectorConfiguration;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Document;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities.DocumentResponse;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities.PagedResponse;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities.TagResponse;
import consulting.erhardt.paperless_ai_flow.services.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Duration;

@ActiveProfiles("test")
@SpringBootTest(
  properties = {
    "paperless.pipelines[0].name=test-pipeline",
    "paperless.pipelines[0].selector.required-tags[0]=INPUT",
    "paperless.pipelines[0].patches[0].action=add",
    "paperless.pipelines[0].patches[0].type=tag",
    "paperless.pipelines[0].patches[0].name=PROCESSED",
    "paperless.pipelines[0].removeInboxTags=true",
  }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ExtendWith(WireMockExtension.class)
class DocumentPollingIntegrationConfigTest {

  @RegisterExtension
  static final WireMockExtension WIRE_MOCK = WireMockExtension.newInstance()
    .options(wireMockConfig().dynamicPort())
    .build();

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("paperless.api.base-url", () -> WIRE_MOCK.getRuntimeInfo().getHttpBaseUrl());
    registry.add("paperless.api.token", () -> "test-token");
  }

  @Autowired
  private DocumentPollingIntegrationConfig pollingIntegrationConfig;

  @Autowired
  private PipelineConfiguration pipelineConfiguration;

  @Autowired
  private PollableChannel pollingChannel;

  @Autowired
  private DocumentPollingService documentPollingService;

  @Autowired
  private IdLockRegistryService<Integer> documentLockRegistry;

  @MockitoBean
  private PdfOcrService pdfOcrService;

  @MockitoBean
  private DocumentMetadataExtractionService documentMetadataExtractionService;

  @MockitoBean
  private DocumentFieldPatchingService documentFieldPatchingService;

  @Autowired
  private consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services.DocumentService documentService;

  @BeforeEach
  void setUpMocks() {
    WIRE_MOCK.resetAll();
    when(pdfOcrService.processDocument(any(Document.class), any()))
      .thenAnswer(invocation -> {
        var doc = (Document) invocation.getArgument(0);
        return doc != null ? Mono.just("ocr-result-" + doc.getId()) : Mono.just("ocr-result");
      });
    when(documentMetadataExtractionService.extractMetadata(any(), any()))
      .thenAnswer(invocation -> {
        var doc = invocation.getArgument(1, Document.class);
        return doc != null ? Mono.just(doc) : Mono.error(new IllegalArgumentException("Document is null"));
      });
    when(documentFieldPatchingService.applyPatches(any(Document.class), any()))
      .thenAnswer(invocation -> {
        var doc = invocation.getArgument(0, Document.class);
        return doc != null ? Mono.just(doc) : Mono.error(new IllegalArgumentException("Document is null"));
      });
  }

  @AfterEach
  void tearDown() {
    while (pollingChannel.receive(0) != null) {
      // drain the queue
    }
  }

  @Test
  void pollDocuments_shouldLockAndUnlockDocuments() throws Exception {
    stubTagPages();
    stubDocumentPage(1, List.of(
      documentResponse(101, "Doc-101"),
      documentResponse(102, "Doc-102")), null);

    // Stub the document saves
    stubDocumentPatch(101);
    stubDocumentPatch(102);

    pollingIntegrationConfig.pollDocuments();

    // Wait for async processing to complete and documents to be unlocked
    await().atMost(Duration.ofSeconds(5))
      .untilAsserted(() -> {
        assertThat(documentLockRegistry.isLocked(101)).isFalse();
        assertThat(documentLockRegistry.isLocked(102)).isFalse();
      });
  }

  @Test
  void pollDocuments_shouldSkipAlreadyLockedDocuments() throws Exception {
    stubTagPages();

    // Pre-lock document 150
    documentLockRegistry.tryLock(150);

    stubDocumentPage(1, List.of(
      documentResponse(150, "Locked"),
      documentResponse(151, "Unlocked")), null);

    // Stub the document save for document 151
    stubDocumentPatch(151);

    pollingIntegrationConfig.pollDocuments();

    // Document 150 should still be locked (wasn't processed)
    assertThat(documentLockRegistry.isLocked(150)).isTrue();

    // Wait for document 151 to be processed and unlocked
    await().atMost(Duration.ofSeconds(5))
      .untilAsserted(() -> assertThat(documentLockRegistry.isLocked(151)).isFalse());
  }

  @Test
  void pollDocuments_shouldRespectQueueCapacity() throws Exception {
    stubTagPages();

    // Verify that polling respects the queue's remaining capacity
    // Queue capacity is 2, so polling should stop after enqueuing 2 documents
    stubDocumentPage(1, List.of(
      documentResponse(200, "Doc-1"),
      documentResponse(201, "Doc-2"),
      documentResponse(202, "Doc-3")),
      WIRE_MOCK.getRuntimeInfo().getHttpBaseUrl() + "/api/documents/?page=2");
    stubDocumentPage(2, List.of(documentResponse(203, "Doc-4")), null);

    // Stub document saves
    stubDocumentPatch(200);
    stubDocumentPatch(201);

    pollingIntegrationConfig.pollDocuments();

    // Wait for documents to be processed
    await().atMost(Duration.ofSeconds(5))
      .untilAsserted(() -> {
        assertThat(documentLockRegistry.isLocked(200)).isFalse();
        assertThat(documentLockRegistry.isLocked(201)).isFalse();
      });

    // Documents 202 and 203 should never have been locked (beyond capacity)
    assertThat(documentLockRegistry.isLocked(202)).isFalse();
    assertThat(documentLockRegistry.isLocked(203)).isFalse();

    // Page 2 should not have been fetched (stopped after capacity reached)
    WIRE_MOCK.verify(0, getRequestedFor(urlPathEqualTo("/api/documents/"))
      .withQueryParam("page", equalTo("2")));
  }

  @Test
  void processDocumentOcr_shouldUnlockDocumentOnError() {
    var pipeline = pipelineConfiguration.getPipelines().getFirst();
    var document = Document.builder().id(300).title("Error document").build();
    documentLockRegistry.tryLock(300);

    when(pdfOcrService.processDocument(any(Document.class), any()))
      .thenReturn(Mono.error(new IllegalStateException("OCR failed")));

    Message<Document> message = MessageBuilder.withPayload(document)
      .setHeader("pipeline", pipeline)
      .setHeader("pipelineName", pipeline.getName())
      .build();

    var result = pollingIntegrationConfig.processDocumentOcr(message);

    assertThat(result).isNull();
    assertThat(documentLockRegistry.isLocked(300)).isFalse();
  }

  @Test
  void fullPipeline_shouldProcessAndUnlockSuccessfully() throws Exception {
    var pipeline = pipelineConfiguration.getPipelines().getFirst();
    var documentId = 400;
    var document = Document.builder()
      .id(documentId)
      .title("Full pipeline test")
      .build();

    // Lock document as if polling had locked it
    documentLockRegistry.tryLock(documentId);

    // Stub all stages to succeed
    when(pdfOcrService.processDocument(any(Document.class), any()))
      .thenReturn(Mono.just("OCR processed text"));

    when(documentMetadataExtractionService.extractMetadata(any(), any()))
      .thenAnswer(invocation -> Mono.just(invocation.getArgument(1)));

    when(documentFieldPatchingService.applyPatches(any(Document.class), any()))
      .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    // Stub the final document save with WireMock
    stubDocumentPatch(documentId);

    // Build message and process through entire pipeline
    Message<Document> message = MessageBuilder.withPayload(document)
      .setHeader("pipeline", pipeline)
      .setHeader("pipelineName", pipeline.getName())
      .build();

    // Process through OCR stage
    var ocrResult = pollingIntegrationConfig.processDocumentOcr(message);
    assertThat(ocrResult).isNotNull();

    // Process through metadata extraction stage
    var metadataResult = pollingIntegrationConfig.processMetadataExtraction(ocrResult);
    assertThat(metadataResult).isNotNull();

    // Process through patching stage
    var patchingResult = pollingIntegrationConfig.processPatching(metadataResult);
    assertThat(patchingResult).isNotNull();

    // Process through final save stage
    pollingIntegrationConfig.handleFinishedDocument(patchingResult);

    // Verify document was unlocked after successful processing
    assertThat(documentLockRegistry.isLocked(documentId)).isFalse();

    // Verify WireMock was called for the document save
    WIRE_MOCK.verify(1, patchRequestedFor(urlPathEqualTo("/api/documents/" + documentId + "/")));
  }

  @Test
  void processMetadataExtraction_shouldUnlockOnFailure() {
    var pipeline = pipelineConfiguration.getPipelines().getFirst();
    var documentId = 500;
    var document = Document.builder()
      .id(documentId)
      .title("Metadata extraction error")
      .build();

    documentLockRegistry.tryLock(documentId);

    when(documentMetadataExtractionService.extractMetadata(any(), any()))
      .thenReturn(Mono.error(new RuntimeException("Metadata extraction failed")));

    Message<Document> message = MessageBuilder.withPayload(document)
      .setHeader("pipeline", pipeline)
      .setHeader("pipelineName", pipeline.getName())
      .build();

    var result = pollingIntegrationConfig.processMetadataExtraction(message);

    assertThat(result).isNull();
    assertThat(documentLockRegistry.isLocked(documentId)).isFalse();
  }

  @Test
  void processPatching_shouldUnlockOnFailure() {
    var pipeline = pipelineConfiguration.getPipelines().getFirst();
    var documentId = 600;
    var document = Document.builder()
      .id(documentId)
      .title("Patching error")
      .build();

    documentLockRegistry.tryLock(documentId);

    when(documentFieldPatchingService.applyPatches(any(Document.class), any()))
      .thenReturn(Mono.error(new RuntimeException("Patching failed")));

    Message<Document> message = MessageBuilder.withPayload(document)
      .setHeader("pipeline", pipeline)
      .setHeader("pipelineName", pipeline.getName())
      .build();

    var result = pollingIntegrationConfig.processPatching(message);

    assertThat(result).isNull();
    assertThat(documentLockRegistry.isLocked(documentId)).isFalse();
  }

  @Test
  void handleFinishedDocument_shouldUnlockOnSaveFailure() throws Exception {
    var pipeline = pipelineConfiguration.getPipelines().getFirst();
    var documentId = 700;
    var document = Document.builder()
      .id(documentId)
      .title("Save error")
      .build();

    documentLockRegistry.tryLock(documentId);

    // Stub WireMock to return error for document patch
    WIRE_MOCK.stubFor(patch(urlPathEqualTo("/api/documents/" + documentId + "/"))
      .willReturn(aResponse()
        .withStatus(500)
        .withHeader("Content-Type", "application/json")
        .withBody("{\"error\":\"Internal server error\"}")));

    Message<Document> message = MessageBuilder.withPayload(document)
      .setHeader("pipeline", pipeline)
      .setHeader("pipelineName", pipeline.getName())
      .build();

    // This should handle the error gracefully and unlock
    pollingIntegrationConfig.handleFinishedDocument(message);

    // Verify document was unlocked despite save failure
    assertThat(documentLockRegistry.isLocked(documentId)).isFalse();

    // Verify the patch was attempted
    WIRE_MOCK.verify(1, patchRequestedFor(urlPathEqualTo("/api/documents/" + documentId + "/")));
  }

  private void stubTagPages() throws Exception {
    var tagResponse = TagResponse.builder()
      .id(1)
      .slug("input")
      .name("INPUT")
      .color("#FFF")
      .textColor("#000")
      .build();

    var tagsBody = OBJECT_MAPPER.writeValueAsString(PagedResponse.<TagResponse>builder()
      .count(1)
      .next(null)
      .previous(null)
      .results(List.of(tagResponse))
      .build());

    WIRE_MOCK.stubFor(get(urlPathEqualTo("/api/tags/"))
      .withQueryParam("page", equalTo("1"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "application/json")
        .withBody(tagsBody)));

    WIRE_MOCK.stubFor(get(urlPathMatching("/api/tags/\\d+/"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "application/json")
        .withBody(OBJECT_MAPPER.writeValueAsString(tagResponse))));
  }

  private void stubDocumentPage(int page, List<DocumentResponse> documents, String next) throws Exception {
    var body = OBJECT_MAPPER.writeValueAsString(PagedResponse.<DocumentResponse>builder()
      .count(documents.size())
      .next(next)
      .previous(null)
      .results(documents)
      .build());

    WIRE_MOCK.stubFor(get(urlPathEqualTo("/api/documents/"))
      .withQueryParam("page", equalTo(String.valueOf(page)))
      .withQueryParam("tags__id__all", equalTo("1"))
      .withQueryParam("ordering", equalTo("-added"))
      .willReturn(aResponse()
        .withHeader("Content-Type", "application/json")
        .withBody(body)));
  }

  private List<Integer> receiveDocumentIds() {
    var ids = new AtomicInteger[0];
    Message<?> message;
    var collected = new java.util.ArrayList<Integer>();
    while ((message = pollingChannel.receive(0)) != null) {
      var payload = (Document) message.getPayload();
      collected.add(payload.getId());
    }
    return collected;
  }

  private void prefillQueueToCapacity() {
    pollingChannel.send(buildMessageFor(Document.builder().id(1).title("Existing-1").build()));
    pollingChannel.send(buildMessageFor(Document.builder().id(2).title("Existing-2").build()));
  }

  private Message<Document> buildMessageFor(Document document) {
    var pipeline = pipelineConfiguration.getPipelines().getFirst();
    return MessageBuilder.withPayload(document)
      .setHeader("pipeline", pipeline)
      .setHeader("pipelineName", pipeline.getName())
      .build();
  }

  private DocumentResponse documentResponse(int id, String title) {
    return DocumentResponse.builder()
      .id(id)
      .title(title)
      .tagIds(List.of())
      .customFields(List.of())
      .build();
  }

  private void stubDocumentPatch(int documentId) throws Exception {
    var patchedDocument = DocumentResponse.builder()
      .id(documentId)
      .title("Patched document")
      .tagIds(List.of())
      .customFields(List.of())
      .build();

    WIRE_MOCK.stubFor(patch(urlMatching("/api/documents/" + documentId + "/.*"))
      .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody(OBJECT_MAPPER.writeValueAsString(patchedDocument))));
  }
}
