package consulting.erhardt.paperless_ai_flow.services;

import consulting.erhardt.paperless_ai_flow.ai.dtos.*;
import consulting.erhardt.paperless_ai_flow.ai.models.*;
import consulting.erhardt.paperless_ai_flow.configs.PipelineConfiguration;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Correspondent;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.CustomField;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Document;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Tag;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services.CorrespondentService;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services.CustomFieldsService;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services.TagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DocumentMetadataExtractionService using mocked AI models.
 */
@ExtendWith(MockitoExtension.class)
class DocumentMetadataExtractionServiceTest {

  @Mock
  private TitleExtractionModel titleModel;

  @Mock
  private TagExtractionModel tagModel;

  @Mock
  private CorrespondentExtractionModel correspondentModel;

  @Mock
  private CustomFieldExtractionModel customFieldModel;

  @Mock
  private CreatedDateExtractionModel createdDateModel;

  @Mock
  private TagService tagService;

  @Mock
  private CorrespondentService correspondentService;

  @Mock
  private CustomFieldsService customFieldsService;

  private DocumentMetadataExtractionService extractionService;

  @BeforeEach
  void setUp() {
    extractionService = new DocumentMetadataExtractionService(
      titleModel,
      tagModel,
      correspondentModel,
      customFieldModel,
      tagService,
      correspondentService,
      customFieldsService,
      createdDateModel
    );
  }

  @Test
  @DisplayName("Should extract all metadata when all extractions enabled")
  void extractMetadata_allEnabled_extractsAllFields() throws IOException {
    // Arrange
    var document = Document.builder()
      .id(123)
      .content("Invoice from Company X dated 2025-01-15 for €1000")
      .build();

    var extraction = PipelineConfiguration.ExtractionConfiguration.builder()
      .title(true)
      .createdDate(true)
      .tags(true)
      .correspondent(true)
      .customFields(true)
      .build();

    var selector = PipelineConfiguration.SelectorConfiguration.builder()
      .requiredTags(List.of("test"))
      .build();
    var pipeline = PipelineConfiguration.PipelineDefinition.builder()
      .name("test-pipeline")
      .selector(selector)
      .extraction(extraction)
      .build();

    // Mock title extraction
    when(titleModel.process(anyString(), isNull()))
      .thenReturn(new TitleExtraction("Invoice Company X 2025-01-15"));

    // Mock created date extraction
    when(createdDateModel.process(anyString(), isNull()))
      .thenReturn(CreatedDateExtraction.builder()
        .createdDate(LocalDate.of(2025, 1, 15))
        .build());

    // Mock tags extraction
    var tagExtraction = TagsExtraction.builder().tagIds(List.of(1, 2)).build();
    when(tagModel.process(anyString(), isNull())).thenReturn(tagExtraction);
    when(tagService.getById(1)).thenReturn(Mono.just(
      Tag.builder().id(1).name("Invoice").slug("invoice").color("#FF0000").textColor("#FFFFFF").build()
    ));
    when(tagService.getById(2)).thenReturn(Mono.just(
      Tag.builder().id(2).name("Financial").slug("financial").color("#00FF00").textColor("#000000").build()
    ));

    // Mock correspondent extraction
    var correspondentExtraction = CorrespondentExtraction.builder().correspondentId(5).build();
    when(correspondentModel.process(anyString(), isNull())).thenReturn(correspondentExtraction);
    when(correspondentService.getById(5)).thenReturn(Mono.just(
      Correspondent.builder().id(5).name("Company X").slug("company-x").build()
    ));

    // Mock custom fields extraction
    var customFieldsExtraction = CustomFieldsExtraction.builder()
      .customFields(Map.of(1, "12345", 2, "1000.00"))
      .build();
    when(customFieldModel.process(anyString(), isNull())).thenReturn(customFieldsExtraction);
    when(customFieldsService.getById(1)).thenReturn(Mono.just(
      CustomField.builder().id(1).name("Invoice Number").dataType("string").build()
    ));
    when(customFieldsService.getById(2)).thenReturn(Mono.just(
      CustomField.builder().id(2).name("Amount").dataType("monetary").build()
    ));

    // Act & Assert
    StepVerifier.create(extractionService.extractMetadata(pipeline, document))
      .assertNext(result -> {
        assertEquals("Invoice Company X 2025-01-15", result.getTitle());
        assertEquals(LocalDate.of(2025, 1, 15), result.getCreatedDate());
        assertEquals(2, result.getTags().size());
        assertEquals("Company X", result.getCorrespondent().getName());
        assertEquals(2, result.getCustomFields().size());
      })
      .verifyComplete();
  }

  @Test
  @DisplayName("Should skip disabled extractions")
  void extractMetadata_someDisabled_skipsDisabledFields() throws IOException {
    // Arrange
    var document = Document.builder()
      .id(123)
      .content("Test content")
      .build();

    var extraction = PipelineConfiguration.ExtractionConfiguration.builder()
      .title(true)
      .createdDate(false)
      .tags(false)
      .correspondent(false)
      .customFields(false)
      .build();

    var selector = PipelineConfiguration.SelectorConfiguration.builder()
      .requiredTags(List.of("test"))
      .build();
    var pipeline = PipelineConfiguration.PipelineDefinition.builder()
      .name("test-pipeline")
      .selector(selector)
      .extraction(extraction)
      .build();

    when(titleModel.process(anyString(), isNull()))
      .thenReturn(new TitleExtraction("Test Title"));

    // Act & Assert
    StepVerifier.create(extractionService.extractMetadata(pipeline, document))
      .assertNext(result -> {
        assertEquals("Test Title", result.getTitle());
        assertNull(result.getCreatedDate());
        assertNull(result.getTags());
        assertNull(result.getCorrespondent());
        assertNull(result.getCustomFields());
      })
      .verifyComplete();
  }

  @Test
  @DisplayName("Should handle null extraction results")
  void extractMetadata_nullResults_skipsNullFields() throws IOException {
    // Arrange
    var document = Document.builder()
      .id(123)
      .content("Test content")
      .title("Original Title")
      .build();

    var extraction = PipelineConfiguration.ExtractionConfiguration.builder()
      .title(true)
      .createdDate(true)
      .tags(false)
      .correspondent(false)
      .customFields(false)
      .build();

    var selector = PipelineConfiguration.SelectorConfiguration.builder()
      .requiredTags(List.of("test"))
      .build();
    var pipeline = PipelineConfiguration.PipelineDefinition.builder()
      .name("test-pipeline")
      .selector(selector)
      .extraction(extraction)
      .build();

    // Return null results
    when(titleModel.process(anyString(), isNull())).thenReturn(null);
    when(createdDateModel.process(anyString(), isNull())).thenReturn(null);

    // Act & Assert
    StepVerifier.create(extractionService.extractMetadata(pipeline, document))
      .assertNext(result -> {
        // Original title remains when extraction returns null
        assertEquals("Original Title", result.getTitle());
        assertNull(result.getCreatedDate());
      })
      .verifyComplete();
  }

  @Test
  @DisplayName("Should handle all extractions disabled")
  void extractMetadata_allDisabled_returnsOriginalDocument() {
    // Arrange
    var document = Document.builder()
      .id(123)
      .content("Test content")
      .title("Original Title")
      .build();

    var extraction = PipelineConfiguration.ExtractionConfiguration.builder()
      .title(false)
      .createdDate(false)
      .tags(false)
      .correspondent(false)
      .customFields(false)
      .build();

    var selector = PipelineConfiguration.SelectorConfiguration.builder()
      .requiredTags(List.of("test"))
      .build();
    var pipeline = PipelineConfiguration.PipelineDefinition.builder()
      .name("test-pipeline")
      .selector(selector)
      .extraction(extraction)
      .build();

    // Act & Assert
    StepVerifier.create(extractionService.extractMetadata(pipeline, document))
      .assertNext(result -> {
        assertEquals("Original Title", result.getTitle());
        assertEquals(123, result.getId());
      })
      .verifyComplete();
  }

  @Test
  @DisplayName("Should handle tag resolution with partial failures")
  void extractMetadata_tagResolutionPartiallyFails_returnsFoundTags() throws IOException {
    // Arrange
    var document = Document.builder()
      .id(123)
      .content("Test content")
      .build();

    var extraction = PipelineConfiguration.ExtractionConfiguration.builder()
      .title(false)
      .createdDate(false)
      .tags(true)
      .correspondent(false)
      .customFields(false)
      .build();

    var selector = PipelineConfiguration.SelectorConfiguration.builder()
      .requiredTags(List.of("test"))
      .build();
    var pipeline = PipelineConfiguration.PipelineDefinition.builder()
      .name("test-pipeline")
      .selector(selector)
      .extraction(extraction)
      .build();

    var tagExtraction = TagsExtraction.builder().tagIds(List.of(1, 999, 3)).build();
    when(tagModel.process(anyString(), isNull())).thenReturn(tagExtraction);
    when(tagService.getById(1)).thenReturn(Mono.just(
      Tag.builder().id(1).name("Tag1").slug("tag1").color("#FF0000").textColor("#FFFFFF").build()
    ));
    when(tagService.getById(999)).thenReturn(Mono.error(new RuntimeException("Not found")));
    when(tagService.getById(3)).thenReturn(Mono.just(
      Tag.builder().id(3).name("Tag3").slug("tag3").color("#00FF00").textColor("#000000").build()
    ));

    // Act & Assert
    StepVerifier.create(extractionService.extractMetadata(pipeline, document))
      .assertNext(result -> {
        assertNotNull(result.getTags());
        assertEquals(2, result.getTags().size()); // Only successfully resolved tags
        assertTrue(result.getTags().stream().anyMatch(t -> t.getName().equals("Tag1")));
        assertTrue(result.getTags().stream().anyMatch(t -> t.getName().equals("Tag3")));
      })
      .verifyComplete();
  }

  @Test
  @DisplayName("Should handle custom fields with integer keys")
  void extractMetadata_customFieldsWithIntKeys_extractsCorrectly() throws IOException {
    // Arrange
    var document = Document.builder()
      .id(123)
      .content("Invoice #INV-12345 Amount: €1000.00")
      .build();

    var extraction = PipelineConfiguration.ExtractionConfiguration.builder()
      .title(false)
      .createdDate(false)
      .tags(false)
      .correspondent(false)
      .customFields(true)
      .build();

    var selector = PipelineConfiguration.SelectorConfiguration.builder()
      .requiredTags(List.of("test"))
      .build();
    var pipeline = PipelineConfiguration.PipelineDefinition.builder()
      .name("test-pipeline")
      .selector(selector)
      .extraction(extraction)
      .build();

    var customFieldsExtraction = CustomFieldsExtraction.builder()
      .customFields(Map.of(1, "INV-12345", 2, "1000.00"))
      .build();
    when(customFieldModel.process(anyString(), isNull())).thenReturn(customFieldsExtraction);
    when(customFieldsService.getById(1)).thenReturn(Mono.just(
      CustomField.builder().id(1).name("Invoice Number").dataType("string").build()
    ));
    when(customFieldsService.getById(2)).thenReturn(Mono.just(
      CustomField.builder().id(2).name("Amount").dataType("monetary").build()
    ));

    // Act & Assert
    StepVerifier.create(extractionService.extractMetadata(pipeline, document))
      .assertNext(result -> {
        assertNotNull(result.getCustomFields());
        assertEquals(2, result.getCustomFields().size());
        assertTrue(result.getCustomFields().stream()
          .anyMatch(cf -> cf.getName().equals("Invoice Number") && cf.getValue().equals("INV-12345")));
        assertTrue(result.getCustomFields().stream()
          .anyMatch(cf -> cf.getName().equals("Amount") && cf.getValue().equals("1000.00")));
      })
      .verifyComplete();
  }

  @Test
  @DisplayName("Should use custom prompts when provided")
  void extractMetadata_customPrompts_usesProvidedPrompts() throws IOException {
    // Arrange
    var document = Document.builder()
      .id(123)
      .content("Test content")
      .build();

    var extraction = PipelineConfiguration.ExtractionConfiguration.builder()
      .title(true)
      .titlePrompt("Custom title prompt")
      .createdDate(false)
      .tags(false)
      .correspondent(false)
      .customFields(false)
      .build();

    var selector = PipelineConfiguration.SelectorConfiguration.builder()
      .requiredTags(List.of("test"))
      .build();
    var pipeline = PipelineConfiguration.PipelineDefinition.builder()
      .name("test-pipeline")
      .selector(selector)
      .extraction(extraction)
      .build();

    when(titleModel.process(anyString(), eq("Custom title prompt")))
      .thenReturn(new TitleExtraction("Custom Title"));

    // Act & Assert
    StepVerifier.create(extractionService.extractMetadata(pipeline, document))
      .assertNext(result -> assertEquals("Custom Title", result.getTitle()))
      .verifyComplete();
  }

  @Test
  @DisplayName("Should handle empty tag list from AI")
  void extractMetadata_emptyTagList_returnsDocumentWithoutTags() throws IOException {
    // Arrange
    var document = Document.builder()
      .id(123)
      .content("Test content")
      .build();

    var extraction = PipelineConfiguration.ExtractionConfiguration.builder()
      .title(false)
      .createdDate(false)
      .tags(true)
      .correspondent(false)
      .customFields(false)
      .build();

    var selector = PipelineConfiguration.SelectorConfiguration.builder()
      .requiredTags(List.of("test"))
      .build();
    var pipeline = PipelineConfiguration.PipelineDefinition.builder()
      .name("test-pipeline")
      .selector(selector)
      .extraction(extraction)
      .build();

    var emptyTagExtraction = TagsExtraction.builder().tagIds(List.of()).build();
    when(tagModel.process(anyString(), isNull())).thenReturn(emptyTagExtraction);

    // Act & Assert
    StepVerifier.create(extractionService.extractMetadata(pipeline, document))
      .assertNext(result -> {
        assertNotNull(result.getTags());
        assertTrue(result.getTags().isEmpty());
      })
      .verifyComplete();
  }

  @Test
  @DisplayName("Should handle null correspondent ID")
  void extractMetadata_nullCorrespondentId_skipsCorrespondent() throws IOException {
    // Arrange
    var document = Document.builder()
      .id(123)
      .content("Test content")
      .build();

    var extraction = PipelineConfiguration.ExtractionConfiguration.builder()
      .title(false)
      .createdDate(false)
      .tags(false)
      .correspondent(true)
      .customFields(false)
      .build();

    var selector = PipelineConfiguration.SelectorConfiguration.builder()
      .requiredTags(List.of("test"))
      .build();
    var pipeline = PipelineConfiguration.PipelineDefinition.builder()
      .name("test-pipeline")
      .selector(selector)
      .extraction(extraction)
      .build();

    when(correspondentModel.process(anyString(), isNull()))
      .thenReturn(CorrespondentExtraction.builder().correspondentId(null).build());

    // Act & Assert
    StepVerifier.create(extractionService.extractMetadata(pipeline, document))
      .assertNext(result -> assertNull(result.getCorrespondent()))
      .verifyComplete();
  }

  @Test
  @DisplayName("Should handle custom field resolution failures")
  void extractMetadata_customFieldResolutionFails_returnsPartialCustomFields() throws IOException {
    // Arrange
    var document = Document.builder()
      .id(123)
      .content("Test content")
      .build();

    var extraction = PipelineConfiguration.ExtractionConfiguration.builder()
      .title(false)
      .createdDate(false)
      .tags(false)
      .correspondent(false)
      .customFields(true)
      .build();

    var selector = PipelineConfiguration.SelectorConfiguration.builder()
      .requiredTags(List.of("test"))
      .build();
    var pipeline = PipelineConfiguration.PipelineDefinition.builder()
      .name("test-pipeline")
      .selector(selector)
      .extraction(extraction)
      .build();

    var customFieldsExtraction = CustomFieldsExtraction.builder()
      .customFields(Map.of(1, "value1", 999, "value999", 3, "value3"))
      .build();
    when(customFieldModel.process(anyString(), isNull())).thenReturn(customFieldsExtraction);
    when(customFieldsService.getById(1)).thenReturn(Mono.just(
      CustomField.builder().id(1).name("Field1").dataType("string").build()
    ));
    when(customFieldsService.getById(999)).thenReturn(Mono.error(new RuntimeException("Not found")));
    when(customFieldsService.getById(3)).thenReturn(Mono.just(
      CustomField.builder().id(3).name("Field3").dataType("string").build()
    ));

    // Act & Assert
    StepVerifier.create(extractionService.extractMetadata(pipeline, document))
      .assertNext(result -> {
        assertNotNull(result.getCustomFields());
        assertEquals(2, result.getCustomFields().size()); // Only successfully resolved fields
        assertTrue(result.getCustomFields().stream().anyMatch(cf -> cf.getName().equals("Field1")));
        assertTrue(result.getCustomFields().stream().anyMatch(cf -> cf.getName().equals("Field3")));
      })
      .verifyComplete();
  }
}
