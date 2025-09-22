package consulting.erhardt.paperless_ai_flow.app.service;

import consulting.erhardt.paperless_ai_flow.app.ai.dtos.DocumentMetadataDto;
import consulting.erhardt.paperless_ai_flow.app.ai.models.CorrespondentExtractionModel;
import consulting.erhardt.paperless_ai_flow.app.ai.models.CustomFieldExtractionModel;
import consulting.erhardt.paperless_ai_flow.app.ai.models.TagExtractionModel;
import consulting.erhardt.paperless_ai_flow.app.ai.models.TitleExtractionModel;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Correspondent;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.CustomField;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Tag;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services.CorrespondentService;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services.CustomFieldsService;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services.TagService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;

/**
 * Service for extracting document metadata using parallel AI processing
 * Coordinates title, tags, correspondent, and custom field extraction
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentMetadataExtractionService {

  private final TitleExtractionModel titleModel;
  private final TagExtractionModel tagModel;
  private final CorrespondentExtractionModel correspondentModel;
  private final CustomFieldExtractionModel customFieldModel;
  private final TagService tagService;
  private final CorrespondentService correspondentService;
  private final CustomFieldsService customFieldsService;

  /**
   * Extract metadata from document content using parallel AI processing
   */
  public Mono<DocumentMetadataDto> extractMetadata(@NonNull String content) {
    log.info("Starting parallel metadata extraction for document content (length: {})", content.length());

    // Fetch available options from Paperless API in parallel
    var tagsMono = tagService.getAll();
    var correspondentsMono = correspondentService.getAll();
    var customFieldsMono = customFieldsService.getAll();

    return Mono.zip(tagsMono, correspondentsMono, customFieldsMono)
      .flatMap(tuple -> {
        var availableTags = tuple.getT1();
        var availableCorrespondents = tuple.getT2();
        var availableCustomFields = tuple.getT3();

        log.debug("Fetched available options - Tags: {}, Correspondents: {}, Custom fields: {}",
          availableTags.size(), availableCorrespondents.size(), availableCustomFields.size());

        // Run AI extractions in parallel
        var titleMono = extractTitle(content);
        var tagsMono2 = extractTags(content, availableTags);
        var correspondentMono = extractCorrespondent(content, availableCorrespondents);
        var customFieldsMono2 = extractCustomFields(content, availableCustomFields);

        return Mono.zip(titleMono, tagsMono2, correspondentMono, customFieldsMono2)
          .map(results -> {
            var title = results.getT1();
            var tagIds = results.getT2();
            var correspondentId = results.getT3();
            var customFields = results.getT4();

            // Convert sentinel value back to null
            var actualCorrespondentId = correspondentId != null && correspondentId == -1L ? null : correspondentId;

            log.info("Metadata extraction completed - Title: '{}', Tags: {}, Correspondent: {}, Custom fields: {}",
              title, tagIds.size(), actualCorrespondentId, customFields.size());

            return DocumentMetadataDto.builder()
              .title(title)
              .tagIds(tagIds)
              .correspondentId(actualCorrespondentId)
              .customFields(customFields)
              .build();
          });
      });
  }

  private Mono<String> extractTitle(String content) {
    return Mono.fromCallable(() -> titleModel.process(content))
      .doOnSubscribe(sub -> log.debug("Starting title extraction"))
      .doOnNext(title -> log.debug("Title extracted: '{}'", title))
      .onErrorResume(error -> {
        log.error("Title extraction failed: {}", error.getMessage(), error);
        return Mono.just("Document Title"); // Fallback
      });
  }

  private Mono<List<Long>> extractTags(String content, List<Tag> availableTags) {
    if (availableTags.isEmpty()) {
      log.warn("No tags available for extraction");
      return Mono.just(List.of());
    }

    return Mono.fromCallable(() -> tagModel.process(content, availableTags))
      .doOnSubscribe(sub -> log.debug("Starting tag extraction with {} available tags", availableTags.size()))
      .doOnNext(tagIds -> log.debug("Tags extracted: {} IDs", tagIds.size()))
      .onErrorResume(error -> {
        log.error("Tag extraction failed: {}", error.getMessage(), error);
        return Mono.just(List.of()); // Fallback to empty list
      });
  }

  private Mono<Long> extractCorrespondent(String content, List<Correspondent> availableCorrespondents) {
    if (availableCorrespondents.isEmpty()) {
      log.warn("No correspondents available for extraction");
      return Mono.just(-1L); // Sentinel value for no correspondent
    }

    return Mono.fromCallable(() -> correspondentModel.process(content, availableCorrespondents))
      .map(id -> id != null ? id : -1L) // Convert null to sentinel value
      .doOnSubscribe(sub -> log.debug("Starting correspondent extraction with {} available correspondents", availableCorrespondents.size()))
      .doOnNext(correspondentId -> log.debug("Correspondent extracted: {}", correspondentId))
      .onErrorResume(error -> {
        log.error("Correspondent extraction failed: {}", error.getMessage(), error);
        return Mono.just(-1L); // Fallback to sentinel value
      });
  }

  private Mono<java.util.Map<Long, String>> extractCustomFields(String content, List<CustomField> availableCustomFields) {
    if (availableCustomFields.isEmpty()) {
      log.warn("No custom fields available for extraction");
      return Mono.just(new HashMap<>());
    }

    return Mono.fromCallable(() -> customFieldModel.process(content, availableCustomFields))
      .doOnSubscribe(sub -> log.debug("Starting custom field extraction with {} available fields", availableCustomFields.size()))
      .doOnNext(customFields -> log.debug("Custom fields extracted: {} fields", customFields.size()))
      .onErrorResume(error -> {
        log.error("Custom field extraction failed: {}", error.getMessage(), error);
        return Mono.just(new HashMap<>()); // Fallback to empty map
      });
  }
}
