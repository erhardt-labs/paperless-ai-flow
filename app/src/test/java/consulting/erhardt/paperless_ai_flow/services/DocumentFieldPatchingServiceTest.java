package consulting.erhardt.paperless_ai_flow.services;

import consulting.erhardt.paperless_ai_flow.configs.PipelineConfiguration;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Correspondent;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.CustomField;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Document;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Tag;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services.CorrespondentService;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services.CustomFieldsService;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services.TagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentFieldPatchingServiceTest {

  @Mock
  private TagService tagService;

  @Mock
  private CorrespondentService correspondentService;

  @Mock
  private CustomFieldsService customFieldsService;

  private DocumentFieldPatchingService service;

  private Document testDocument;
  private Tag existingTag;
  private Tag newTag;
  private Correspondent existingCorrespondent;
  private Correspondent newCorrespondent;
  private CustomField existingCustomField;
  private CustomField newCustomFieldDefinition;

  @BeforeEach
  void setUp() {
    service = new DocumentFieldPatchingService(tagService, correspondentService, customFieldsService);

    existingTag = Tag.builder()
      .id(1)
      .name("invoice")
      .build();

    newTag = Tag.builder()
      .id(2)
      .name("receipt")
      .build();

    existingCorrespondent = Correspondent.builder()
      .id(1)
      .name("Old Company")
      .build();

    newCorrespondent = Correspondent.builder()
      .id(2)
      .name("Company X")
      .build();

    existingCustomField = CustomField.builder()
      .id(1)
      .name("price")
      .value("100.00")
      .build();

    newCustomFieldDefinition = CustomField.builder()
      .id(2)
      .name("creator")
      .build();

    testDocument = Document.builder()
      .id(123)
      .title("Test Document")
      .tags(List.of(existingTag))
      .correspondent(existingCorrespondent)
      .customFields(List.of(existingCustomField))
      .build();
  }

  @Test
  void shouldAddNewTag() {
    // Given
    var patch = PipelineConfiguration.PatchConfiguration.builder()
      .action(PipelineConfiguration.PatchAction.ADD)
      .type(PipelineConfiguration.PatchType.TAG)
      .name("receipt")
      .build();

    when(tagService.getByName("receipt")).thenReturn(Mono.just(newTag));

    // When & Then
    StepVerifier.create(service.applyPatches(testDocument, List.of(patch)))
      .expectNextMatches(document -> {
        assertThat(document.getTags()).hasSize(2);
        assertThat(document.getTags()).containsExactlyInAnyOrder(existingTag, newTag);
        return true;
      })
      .verifyComplete();
  }

  @Test
  void shouldNotAddExistingTag() {
    // Given
    var patch = PipelineConfiguration.PatchConfiguration.builder()
      .action(PipelineConfiguration.PatchAction.ADD)
      .type(PipelineConfiguration.PatchType.TAG)
      .name("invoice") // Already exists in document
      .build();

    // When & Then
    StepVerifier.create(service.applyPatches(testDocument, List.of(patch)))
      .expectNextMatches(document -> {
        assertThat(document.getTags()).hasSize(1);
        assertThat(document.getTags()).containsExactly(existingTag);
        return true;
      })
      .verifyComplete();
  }

  @Test
  void shouldDropExistingTag() {
    // Given
    var patch = PipelineConfiguration.PatchConfiguration.builder()
      .action(PipelineConfiguration.PatchAction.DROP)
      .type(PipelineConfiguration.PatchType.TAG)
      .name("invoice")
      .build();

    // When & Then
    StepVerifier.create(service.applyPatches(testDocument, List.of(patch)))
      .expectNextMatches(document -> {
        assertThat(document.getTags()).isEmpty();
        return true;
      })
      .verifyComplete();
  }

  @Test
  void shouldNotDropNonExistentTag() {
    // Given
    var patch = PipelineConfiguration.PatchConfiguration.builder()
      .action(PipelineConfiguration.PatchAction.DROP)
      .type(PipelineConfiguration.PatchType.TAG)
      .name("nonexistent")
      .build();

    // When & Then
    StepVerifier.create(service.applyPatches(testDocument, List.of(patch)))
      .expectNextMatches(document -> {
        assertThat(document.getTags()).hasSize(1);
        assertThat(document.getTags()).containsExactly(existingTag);
        return true;
      })
      .verifyComplete();
  }

  @Test
  void shouldSetCorrespondent() {
    // Given
    var patch = PipelineConfiguration.PatchConfiguration.builder()
      .action(PipelineConfiguration.PatchAction.SET)
      .type(PipelineConfiguration.PatchType.CORRESPONDENT)
      .name("Company X")
      .build();

    when(correspondentService.getByName("Company X")).thenReturn(Mono.just(newCorrespondent));

    // When & Then
    StepVerifier.create(service.applyPatches(testDocument, List.of(patch)))
      .expectNextMatches(document -> {
        assertThat(document.getCorrespondent()).isEqualTo(newCorrespondent);
        return true;
      })
      .verifyComplete();
  }

  @Test
  void shouldKeepExistingCorrespondentWhenNotFound() {
    // Given
    var patch = PipelineConfiguration.PatchConfiguration.builder()
      .action(PipelineConfiguration.PatchAction.SET)
      .type(PipelineConfiguration.PatchType.CORRESPONDENT)
      .name("Nonexistent Company")
      .build();

    when(correspondentService.getByName("Nonexistent Company")).thenReturn(Mono.empty());

    // When & Then
    StepVerifier.create(service.applyPatches(testDocument, List.of(patch)))
      .expectNextMatches(document -> {
        assertThat(document.getCorrespondent()).isEqualTo(existingCorrespondent);
        return true;
      })
      .verifyComplete();
  }

  @Test
  void shouldAddNewCustomField() {
    // Given
    var patch = PipelineConfiguration.PatchConfiguration.builder()
      .action(PipelineConfiguration.PatchAction.ADD)
      .type(PipelineConfiguration.PatchType.CUSTOM_FIELD)
      .name("creator")
      .value("openai")
      .build();

    when(customFieldsService.getByName("creator")).thenReturn(Mono.just(newCustomFieldDefinition));

    // When & Then
    StepVerifier.create(service.applyPatches(testDocument, List.of(patch)))
      .expectNextMatches(document -> {
        assertThat(document.getCustomFields()).hasSize(2);

        var creatorField = document.getCustomFields().stream()
          .filter(cf -> cf.getName().equals("creator"))
          .findFirst()
          .orElse(null);

        assertThat(creatorField).isNotNull();
        assertThat(creatorField.getValue()).isEqualTo("openai");
        assertThat(creatorField.getId()).isEqualTo(2);

        return true;
      })
      .verifyComplete();
  }

  @Test
  void shouldOverwriteExistingCustomField() {
    // Given
    var patch = PipelineConfiguration.PatchConfiguration.builder()
      .action(PipelineConfiguration.PatchAction.ADD)
      .type(PipelineConfiguration.PatchType.CUSTOM_FIELD)
      .name("price") // Already exists
      .value("200.00")
      .build();

    when(customFieldsService.getByName("price")).thenReturn(Mono.just(existingCustomField));

    // When & Then
    StepVerifier.create(service.applyPatches(testDocument, List.of(patch)))
      .expectNextMatches(document -> {
        assertThat(document.getCustomFields()).hasSize(1);

        var priceField = document.getCustomFields().get(0);
        assertThat(priceField.getName()).isEqualTo("price");
        assertThat(priceField.getValue()).isEqualTo("200.00");

        return true;
      })
      .verifyComplete();
  }

  @Test
  void shouldDropExistingCustomField() {
    // Given
    var patch = PipelineConfiguration.PatchConfiguration.builder()
      .action(PipelineConfiguration.PatchAction.DROP)
      .type(PipelineConfiguration.PatchType.CUSTOM_FIELD)
      .name("price")
      .build();

    // When & Then
    StepVerifier.create(service.applyPatches(testDocument, List.of(patch)))
      .expectNextMatches(document -> {
        assertThat(document.getCustomFields()).isEmpty();
        return true;
      })
      .verifyComplete();
  }

  @Test
  void shouldNotDropNonExistentCustomField() {
    // Given
    var patch = PipelineConfiguration.PatchConfiguration.builder()
      .action(PipelineConfiguration.PatchAction.DROP)
      .type(PipelineConfiguration.PatchType.CUSTOM_FIELD)
      .name("nonexistent")
      .build();

    // When & Then
    StepVerifier.create(service.applyPatches(testDocument, List.of(patch)))
      .expectNextMatches(document -> {
        assertThat(document.getCustomFields()).hasSize(1);
        assertThat(document.getCustomFields()).containsExactly(existingCustomField);
        return true;
      })
      .verifyComplete();
  }

  @Test
  void shouldApplyMultiplePatches() {
    // Given
    var patches = List.of(
      PipelineConfiguration.PatchConfiguration.builder()
        .action(PipelineConfiguration.PatchAction.ADD)
        .type(PipelineConfiguration.PatchType.TAG)
        .name("receipt")
        .build(),
      PipelineConfiguration.PatchConfiguration.builder()
        .action(PipelineConfiguration.PatchAction.DROP)
        .type(PipelineConfiguration.PatchType.CUSTOM_FIELD)
        .name("price")
        .build(),
      PipelineConfiguration.PatchConfiguration.builder()
        .action(PipelineConfiguration.PatchAction.SET)
        .type(PipelineConfiguration.PatchType.CORRESPONDENT)
        .name("Company X")
        .build()
    );

    when(tagService.getByName("receipt")).thenReturn(Mono.just(newTag));
    when(correspondentService.getByName("Company X")).thenReturn(Mono.just(newCorrespondent));

    // When & Then
    StepVerifier.create(service.applyPatches(testDocument, patches))
      .expectNextMatches(document -> {
        assertThat(document.getTags()).hasSize(2);
        assertThat(document.getTags()).containsExactlyInAnyOrder(existingTag, newTag);
        assertThat(document.getCorrespondent()).isEqualTo(newCorrespondent);
        assertThat(document.getCustomFields()).isEmpty();
        return true;
      })
      .verifyComplete();
  }

  @Test
  void shouldHandleTagResolutionFailure() {
    // Given
    var patch = PipelineConfiguration.PatchConfiguration.builder()
      .action(PipelineConfiguration.PatchAction.ADD)
      .type(PipelineConfiguration.PatchType.TAG)
      .name("nonexistent")
      .build();

    when(tagService.getByName("nonexistent")).thenReturn(Mono.empty());

    // When & Then
    StepVerifier.create(service.applyPatches(testDocument, List.of(patch)))
      .expectNextMatches(document -> {
        // Document should remain unchanged
        assertThat(document.getTags()).hasSize(1);
        assertThat(document.getTags()).containsExactly(existingTag);
        return true;
      })
      .verifyComplete();
  }

  @Test
  void shouldHandleCustomFieldDefinitionNotFound() {
    // Given
    var patch = PipelineConfiguration.PatchConfiguration.builder()
      .action(PipelineConfiguration.PatchAction.ADD)
      .type(PipelineConfiguration.PatchType.CUSTOM_FIELD)
      .name("nonexistent")
      .value("test")
      .build();

    when(customFieldsService.getByName("nonexistent")).thenReturn(Mono.empty());

    // When & Then
    StepVerifier.create(service.applyPatches(testDocument, List.of(patch)))
      .expectNextMatches(document -> {
        // Document should remain unchanged
        assertThat(document.getCustomFields()).hasSize(1);
        assertThat(document.getCustomFields()).containsExactly(existingCustomField);
        return true;
      })
      .verifyComplete();
  }

  @Test
  void shouldRejectInvalidTagSetAction() {
    // Given
    var patch = PipelineConfiguration.PatchConfiguration.builder()
      .action(PipelineConfiguration.PatchAction.SET) // Invalid for TAG
      .type(PipelineConfiguration.PatchType.TAG)
      .name("receipt")
      .build();

    // When & Then
    StepVerifier.create(service.applyPatches(testDocument, List.of(patch)))
      .expectError(IllegalArgumentException.class)
      .verify();
  }

  @Test
  void shouldRejectInvalidCorrespondentAddAction() {
    // Given
    var patch = PipelineConfiguration.PatchConfiguration.builder()
      .action(PipelineConfiguration.PatchAction.ADD) // Invalid for CORRESPONDENT
      .type(PipelineConfiguration.PatchType.CORRESPONDENT)
      .name("Company X")
      .build();

    // When & Then
    StepVerifier.create(service.applyPatches(testDocument, List.of(patch)))
      .expectError(IllegalArgumentException.class)
      .verify();
  }

  @Test
  void shouldRejectInvalidCorrespondentDropAction() {
    // Given
    var patch = PipelineConfiguration.PatchConfiguration.builder()
      .action(PipelineConfiguration.PatchAction.DROP) // Invalid for CORRESPONDENT
      .type(PipelineConfiguration.PatchType.CORRESPONDENT)
      .name("Company X")
      .build();

    // When & Then
    StepVerifier.create(service.applyPatches(testDocument, List.of(patch)))
      .expectError(IllegalArgumentException.class)
      .verify();
  }

  @Test
  void shouldRejectInvalidCustomFieldSetAction() {
    // Given
    var patch = PipelineConfiguration.PatchConfiguration.builder()
      .action(PipelineConfiguration.PatchAction.SET) // Invalid for CUSTOM_FIELD
      .type(PipelineConfiguration.PatchType.CUSTOM_FIELD)
      .name("creator")
      .value("openai")
      .build();

    // When & Then
    StepVerifier.create(service.applyPatches(testDocument, List.of(patch)))
      .expectError(IllegalArgumentException.class)
      .verify();
  }

  @Test
  void shouldSkipCustomFieldWithNullValue() {
    // Given
    var patch = PipelineConfiguration.PatchConfiguration.builder()
      .action(PipelineConfiguration.PatchAction.ADD)
      .type(PipelineConfiguration.PatchType.CUSTOM_FIELD)
      .name("creator")
      .value(null) // Null value should be skipped
      .build();

    // When & Then
    StepVerifier.create(service.applyPatches(testDocument, List.of(patch)))
      .expectNextMatches(document -> {
        // Document should remain unchanged
        assertThat(document.getCustomFields()).hasSize(1);
        assertThat(document.getCustomFields()).containsExactly(existingCustomField);
        return true;
      })
      .verifyComplete();
  }

  @Test
  void shouldHandleEmptyPatchesList() {
    // When & Then
    StepVerifier.create(service.applyPatches(testDocument, List.of()))
      .expectNext(testDocument)
      .verifyComplete();
  }
}
