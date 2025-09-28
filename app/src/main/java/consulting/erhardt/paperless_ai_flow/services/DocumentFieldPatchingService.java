package consulting.erhardt.paperless_ai_flow.services;

import consulting.erhardt.paperless_ai_flow.configs.PipelineConfiguration;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Correspondent;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.CustomField;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Document;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Tag;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services.CorrespondentService;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services.CustomFieldsService;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services.TagService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentFieldPatchingService {

  private final TagService tagService;
  private final CorrespondentService correspondentService;
  private final CustomFieldsService customFieldsService;

  /**
   * Apply field patches to a document based on pipeline configuration
   *
   * @param document The document to patch
   * @param patches  The list of patches to apply
   * @return A Mono containing the patched document
   */
  public Mono<Document> applyPatches(@NonNull Document document, @NonNull List<PipelineConfiguration.PatchConfiguration> patches) {
    log.debug("Applying {} patches to document ID: {}", patches.size(), document.getId());

    return Flux.fromIterable(patches)
      .reduce(Mono.just(document), (docMono, patch) ->
        docMono.flatMap(doc -> applyPatch(doc, patch))
      )
      .flatMap(mono -> mono)
      .doOnNext(patchedDoc ->
        log.info("Applied {} patches to document ID: {}", patches.size(), patchedDoc.getId())
      )
      .doOnError(error ->
        log.error("Failed to apply patches to document ID: {}", document.getId(), error)
      );
  }

  private Mono<Document> applyPatch(@NonNull Document document, @NonNull PipelineConfiguration.PatchConfiguration patch) {
    log.debug("Applying patch: {} {} {}", patch.getAction(), patch.getType(), patch.getName());

    return switch (patch.getType()) {
      case TAG -> applyTagPatch(document, patch);
      case CORRESPONDENT -> applyCorrespondentPatch(document, patch);
      case CUSTOM_FIELD -> applyCustomFieldPatch(document, patch);
    };
  }

  private Mono<Document> applyTagPatch(@NonNull Document document, @NonNull PipelineConfiguration.PatchConfiguration patch) {
    return switch (patch.getAction()) {
      case ADD -> addTag(document, patch.getName());
      case DROP -> dropTag(document, patch.getName());
      case SET -> Mono.error(new IllegalArgumentException("SET action is not supported for TAG type"));
    };
  }

  private Mono<Document> applyCorrespondentPatch(@NonNull Document document, @NonNull PipelineConfiguration.PatchConfiguration patch) {
    return switch (patch.getAction()) {
      case SET -> setCorrespondent(document, patch.getName());
      case ADD, DROP ->
        Mono.error(new IllegalArgumentException(patch.getAction() + " action is not supported for CORRESPONDENT type"));
    };
  }

  private Mono<Document> applyCustomFieldPatch(@NonNull Document document, @NonNull PipelineConfiguration.PatchConfiguration patch) {
    return switch (patch.getAction()) {
      case ADD -> addCustomField(document, patch.getName(), patch.getValue());
      case DROP -> dropCustomField(document, patch.getName());
      case SET ->
        Mono.error(new IllegalArgumentException("SET action is not supported for CUSTOM_FIELD type. Use ADD to set/overwrite values"));
    };
  }

  private Mono<Document> addTag(@NonNull Document document, @NonNull String tagName) {
    log.debug("Adding tag '{}' to document ID: {}", tagName, document.getId());

    // Check if tag already exists in document
    var existingTag = document.getTags().stream()
      .anyMatch(tag -> tag.getName().equals(tagName));

    if (existingTag) {
      log.debug("Tag '{}' already exists in document ID: {}", tagName, document.getId());
      return Mono.just(document);
    }

    return tagService.getByName(tagName)
      .map(tag -> {
        var updatedTags = new ArrayList<>(document.getTags());
        updatedTags.add(tag);

        return patchDocumentWithTags(document, updatedTags);
      })
      .doOnNext(doc -> log.debug("Successfully added tag '{}' to document ID: {}", tagName, doc.getId()))
      .onErrorResume(error -> {
        log.warn("Failed to resolve tag '{}', skipping addition to document ID: {}", tagName, document.getId(), error);
        return Mono.just(document);
      })
      .switchIfEmpty(Mono.fromRunnable(() ->
        log.warn("Tag '{}' not found, skipping addition to document ID: {}", tagName, document.getId())
      ).then(Mono.just(document)));
  }

  private Mono<Document> dropTag(@NonNull Document document, @NonNull String tagName) {
    log.debug("Dropping tag '{}' from document ID: {}", tagName, document.getId());

    var filteredTags = document.getTags().stream()
      .filter(tag -> !tag.getName().equals(tagName))
      .toList();

    if (filteredTags.size() == document.getTags().size()) {
      log.debug("Tag '{}' not found in document ID: {}, no changes made", tagName, document.getId());
    } else {
      log.debug("Successfully dropped tag '{}' from document ID: {}", tagName, document.getId());
    }

    return Mono.just(patchDocumentWithTags(document, filteredTags));
  }

  private Mono<Document> setCorrespondent(@NonNull Document document, @NonNull String correspondentName) {
    log.debug("Setting correspondent '{}' for document ID: {}", correspondentName, document.getId());

    return correspondentService.getByName(correspondentName)
      .map(correspondent -> patchDocumentWithCorrespondent(document, correspondent))
      .doOnNext(doc -> log.debug("Successfully set correspondent '{}' for document ID: {}", correspondentName, doc.getId()))
      .onErrorResume(error -> {
        log.warn("Failed to resolve correspondent '{}', keeping existing correspondent for document ID: {}", correspondentName, document.getId(), error);
        return Mono.just(document);
      })
      .switchIfEmpty(Mono.fromRunnable(() ->
        log.warn("Correspondent '{}' not found, keeping existing correspondent for document ID: {}", correspondentName, document.getId())
      ).then(Mono.just(document)));
  }

  private Mono<Document> addCustomField(@NonNull Document document, @NonNull String fieldName, String value) {
    log.debug("Adding/updating custom field '{}' with value '{}' for document ID: {}", fieldName, value, document.getId());

    if (value == null) {
      log.warn("Custom field '{}' value is null for document ID: {}, skipping", fieldName, document.getId());
      return Mono.just(document);
    }

    return customFieldsService.getByName(fieldName)
      .map(customFieldDefinition -> {
        // Remove existing custom field with same name if it exists
        var filteredCustomFields = document.getCustomFields().stream()
          .filter(cf -> !cf.getName().equals(fieldName))
          .toList();

        var updatedCustomFields = new ArrayList<CustomField>(filteredCustomFields);

        // Add the new custom field using builder pattern
        updatedCustomFields.add(
          customFieldDefinition.toBuilder()
            .value(value)
            .build()
        );

        return patchDocumentWithCustomFields(document, updatedCustomFields);
      })
      .doOnNext(doc -> log.debug("Successfully added/updated custom field '{}' for document ID: {}", fieldName, doc.getId()))
      .onErrorResume(error -> {
        log.warn("Failed to resolve custom field definition '{}', skipping addition to document ID: {}", fieldName, document.getId(), error);
        return Mono.just(document);
      })
      .switchIfEmpty(Mono.fromRunnable(() ->
        log.warn("Custom field definition '{}' not found, skipping addition to document ID: {}", fieldName, document.getId())
      ).then(Mono.just(document)));
  }

  private Mono<Document> dropCustomField(@NonNull Document document, @NonNull String fieldName) {
    log.debug("Dropping custom field '{}' from document ID: {}", fieldName, document.getId());

    var filteredCustomFields = document.getCustomFields().stream()
      .filter(customField -> !customField.getName().equals(fieldName))
      .toList();

    if (filteredCustomFields.size() == document.getCustomFields().size()) {
      log.debug("Custom field '{}' not found in document ID: {}, no changes made", fieldName, document.getId());
    } else {
      log.debug("Successfully dropped custom field '{}' from document ID: {}", fieldName, document.getId());
    }

    return Mono.just(patchDocumentWithCustomFields(document, filteredCustomFields));
  }

  private Document patchDocumentWithTags(Document original, List<Tag> newTags) {
    return original.toBuilder()
      .tags(newTags)
      .build();
  }

  private Document patchDocumentWithCorrespondent(Document original, Correspondent newCorrespondent) {
    return original.toBuilder()
      .correspondent(newCorrespondent)
      .build();
  }

  private Document patchDocumentWithCustomFields(Document original, List<CustomField> newCustomFields) {
    return original.toBuilder()
      .customFields(newCustomFields)
      .build();
  }
}
