package consulting.erhardt.paperless_ai_flow.app.service;

import consulting.erhardt.paperless_ai_flow.app.ai.dtos.CreatedDateDto;
import consulting.erhardt.paperless_ai_flow.app.ai.dtos.TitleDto;
import consulting.erhardt.paperless_ai_flow.app.ai.models.*;
import consulting.erhardt.paperless_ai_flow.app.config.PipelineConfiguration;
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
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
  private final CreatedDateExtractionModel createdDateExtractionModel;

  /**
   * Extract metadata from document content using parallel AI processing
   */
  public Mono<Document> extractMetadata(
    @NonNull PipelineConfiguration.PipelineDefinition pipeline,
    @NonNull Document document
  ) {
    var extraction = pipeline.getExtraction();
    var content = document.getContent();
    log.info("Starting parallel metadata extraction for document content (length: {})", content.length());

    // Run AI extractions in parallel when enabled
    var titleMono = extraction.isTitle() ?
      extractTitle(content, extraction) : Mono.just(Optional.empty());
    var createdDateMono = extraction.isCreatedDate() ?
      extractCreatedDate(content, extraction) : Mono.just(Optional.empty());
    var tagsMono = extraction.isTags() ?
      extractTags(content, extraction) : Mono.just(Optional.empty());
    var correspondentMono = extraction.isCorrespondent() ?
      extractCorrespondent(content, extraction) : Mono.just(Optional.empty());
    var customFieldsMono = extraction.isCustomFields() ?
      extractCustomFields(content, extraction) : Mono.just(Optional.empty());

    return Mono.zip(titleMono, createdDateMono, tagsMono, correspondentMono, customFieldsMono)
      .map(results -> {
        var title = (Optional<String>) results.getT1();
        var createdDate = (Optional<LocalDate>) results.getT2();
        var tagsOpt = (Optional<List<Tag>>) results.getT3();
        var correspondentOpt = (Optional<Correspondent>) results.getT4();
        var customFieldsOpt = (Optional<List<CustomField>>) results.getT5();

        // create builder object
        var documentBuilder = document.toBuilder();
        title.ifPresent(documentBuilder::title);
        createdDate.ifPresent(documentBuilder::createdDate);
        correspondentOpt.ifPresent(documentBuilder::correspondent);
        tagsOpt.ifPresent(documentBuilder::tags);
        customFieldsOpt.ifPresent(documentBuilder::customFields);

        // build
        var updatedDocument = documentBuilder.build();
        log.info("Metadata extraction completed - Title: '{}', Created date: {}, Tags: {}, Correspondent: {}, Custom fields: {}",
          updatedDocument.getTitle(), updatedDocument.getCreatedDate(), updatedDocument.getTags(), updatedDocument.getCorrespondent(), updatedDocument.getCustomFields());

        return updatedDocument;
      });
  }

  private Mono<Optional<LocalDate>> extractCreatedDate(@NonNull String content, @NonNull PipelineConfiguration.ExtractionConfiguration extraction) {
    return Mono.fromCallable(() -> {
        var result = createdDateExtractionModel.process(content, extraction.getCreatedDatePrompt());
        return Optional.ofNullable(result)
          .map(CreatedDateDto::getCreatedDate);
      })
      .subscribeOn(Schedulers.boundedElastic())
      .doOnSubscribe(sub -> log.debug("Starting created date extraction"))
      .doOnSuccess(opt -> opt.ifPresent(t -> log.debug("Created date extracted: '{}'", t)))
      .onErrorResume(error -> {
        log.error("Title extraction failed: {}", error.getMessage(), error);
        return Mono.just(Optional.empty());
      })
      .switchIfEmpty(Mono.just(Optional.empty()));
  }

  private Mono<Optional<String>> extractTitle(@NonNull String content, @NonNull PipelineConfiguration.ExtractionConfiguration extraction) {
    return Mono.fromCallable(() -> {
        var result = titleModel.process(content, extraction.getTitlePrompt());
        return Optional.ofNullable(result)
          .map(TitleDto::getTitle);
      })
      .subscribeOn(Schedulers.boundedElastic())
      .doOnSubscribe(sub -> log.debug("Starting title extraction"))
      .doOnSuccess(opt -> opt.ifPresent(t -> log.debug("Title extracted: '{}'", t)))
      .onErrorResume(error -> {
        log.error("Title extraction failed: {}", error.getMessage(), error);
        return Mono.just(Optional.empty());
      })
      .switchIfEmpty(Mono.just(Optional.empty()));
  }

  private Mono<Optional<List<Tag>>> extractTags(@NonNull String content, @NonNull PipelineConfiguration.ExtractionConfiguration extraction) {
    return Mono.fromCallable(() -> Optional.ofNullable(tagModel.process(content, extraction.getTagsPrompt())))
      .subscribeOn(Schedulers.boundedElastic())
      .doOnSubscribe(s -> log.debug("Starting tags extraction"))
      .flatMap(optDto -> optDto
        .map(dto -> {
          var ids = dto.getTagIds();

          return Flux.fromIterable(ids)
            .flatMapSequential(id ->
                tagService.getById(id)
                  .doOnNext(tag -> log.debug("Resolved tag id {} -> {}", id, tag))
                  .doOnError(e -> log.error("Failed to resolve tag id {}: {}", id, e.getMessage(), e))
                  .onErrorResume(e -> Mono.empty()),
              Math.min(Math.max(ids.size(), 1), 8)
            )
            .collectList()
            .map(Optional::of)
            .doOnNext(opt -> log.debug("Tags extracted (resolved): {}", opt))
            .onErrorResume(e -> {
              log.error("Tags resolution failed: {}", e.getMessage(), e);
              return Mono.just(Optional.empty());
            });
        })
        .orElse(Mono.just(Optional.empty()))
      )
      .switchIfEmpty(Mono.just(Optional.empty()));
  }

  private Mono<Optional<Correspondent>> extractCorrespondent(@NonNull String content, @NonNull PipelineConfiguration.ExtractionConfiguration extraction) {
    return Mono.fromCallable(() -> Optional.ofNullable(correspondentModel.process(content, extraction.getCorrespondentPrompt())))
      .subscribeOn(Schedulers.boundedElastic())
      .flatMap(opt -> opt
        .map(dto -> correspondentService.getById(dto.getCorrespondentId())
          .map(Optional::of)
          .onErrorResume(e -> {
            log.error("Correspondent getById failed: {}", e.getMessage(), e);

            return Mono.just(Optional.empty());
          })
        )
        .orElse(Mono.just(Optional.empty()))
      )
      .doOnSubscribe(s -> log.debug("Starting correspondent extraction"))
      .onErrorResume(e -> {
        log.error("Correspondent extraction failed: {}", e.getMessage(), e);
        return Mono.just(Optional.empty());
      })
      .switchIfEmpty(Mono.just(Optional.empty()));
  }

  private Mono<Optional<List<CustomField>>> extractCustomFields(@NonNull String content, @NonNull PipelineConfiguration.ExtractionConfiguration extraction) {
    return Mono.fromCallable(() -> Optional.ofNullable(customFieldModel.process(content, extraction.getCustomFieldsPrompt()))) // may be null
      .subscribeOn(Schedulers.boundedElastic())
      .doOnSubscribe(s -> log.debug("Starting custom fields extraction"))
      .flatMap(optDto -> optDto
        .map(dto -> {
          var customFields = dto.getCustomFields();

          final List<Map.Entry<Integer, String>> entries = customFields.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .toList();

          return Flux.fromIterable(entries)
            .flatMapSequential(entry -> customFieldsService.getById(entry.getKey())
                  .map(customField -> (CustomField) customField.toBuilder()
                    .value(entry.getValue())
                    .build()
                  )
                  .doOnNext(customField -> log.debug("Resolved custom field id {} -> {}", customField.getName(), customField.getValue()))
                  .doOnError(e -> log.error("Failed to resolve custom field id {}: {}", entry.getKey(), e.getMessage(), e))
                  .onErrorResume(e -> Mono.empty()),
              Math.min(Math.max(entries.size(), 1), 8)
            )
            .collectList()
            .map(Optional::of)
            .doOnNext(opt -> log.debug("Custom fields extracted (resolved): {}", opt))
            .onErrorResume(e -> {
              log.error("Tags resolution failed: {}", e.getMessage(), e);
              return Mono.just(Optional.empty());
            });
        })
        .orElse(Mono.just(Optional.empty()))
      )
      .switchIfEmpty(Mono.just(Optional.empty()));
  }
}
