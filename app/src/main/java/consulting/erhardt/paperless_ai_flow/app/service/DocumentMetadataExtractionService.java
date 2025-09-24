package consulting.erhardt.paperless_ai_flow.app.service;

import consulting.erhardt.paperless_ai_flow.app.ai.dtos.*;
import consulting.erhardt.paperless_ai_flow.app.ai.models.CorrespondentExtractionModel;
import consulting.erhardt.paperless_ai_flow.app.ai.models.CustomFieldExtractionModel;
import consulting.erhardt.paperless_ai_flow.app.ai.models.TagExtractionModel;
import consulting.erhardt.paperless_ai_flow.app.ai.models.TitleExtractionModel;
import consulting.erhardt.paperless_ai_flow.app.config.PipelineConfiguration;
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
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.List;
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

  /**
   * Extract metadata from document content using parallel AI processing
   */
  public Mono<DocumentMetadataDto> extractMetadata(@NonNull PipelineConfiguration.PipelineDefinition pipeline, @NonNull String content) {
    var extraction = pipeline.getExtraction();
    log.info("Starting parallel metadata extraction for document content (length: {})", content.length());

    // Run AI extractions in parallel when enabled
    var titleMono = extraction.isTitle() ?
      extractTitle(content) : Mono.just(Optional.empty());
    var tagsMono = extraction.isTags() ?
      extractTags(content) : Mono.just(Optional.empty());
    var correspondentMono = extraction.isCorrespondent() ?
      extractCorrespondent(content) : Mono.just(Optional.empty());
    var customFieldsMono = extraction.isCustomFields() ?
      extractCustomFields(content) : Mono.just(Optional.empty());

    return Mono.zip(titleMono, tagsMono, correspondentMono, customFieldsMono)
      .map(results -> {
        var title = results.getT1();
        var tagIds = results.getT2();
        var correspondentId = results.getT3();
        var customFields = results.getT4();

        // create metadata object
        var metadataBuilder = DocumentMetadataDto.builder();

        // title
        title.ifPresent(t -> metadataBuilder.title((
          (TitleDto) t).getTitle()
        ));

        // correspondent
        correspondentId.ifPresent(t -> metadataBuilder.correspondentId((
          (CorrespondentDto) t).getCorrespondentId()
        ));

        // tagsIds
        tagIds.ifPresent(t -> metadataBuilder.tagIds((
          (TagsDto) t).getTagIds()
        ));

        // custom fields
        customFields.ifPresent(t -> metadataBuilder.customFields((
          (CustomFieldsDto) t).getCustomFields()
        ));

        // build
        var metadata = metadataBuilder.build();

        log.info("Metadata extraction completed - Title: '{}', Tags: {}, Correspondent: {}, Custom fields: {}",
          metadata.getTitle(), metadata.getTagIds(), metadata.getCorrespondentId(), metadata.getCustomFields());

        return metadata;
      });
  }

  private Mono<Optional<TitleDto>> extractTitle(@NonNull String content) {
    return Mono.fromCallable(() -> {
        var result = titleModel.process(content);

        return Optional.ofNullable(result);
      })
      .subscribeOn(Schedulers.boundedElastic())
      .doOnSubscribe(sub -> log.debug("Starting title extraction"))
      .doOnSuccess(opt -> opt.ifPresent(t -> log.debug("Title extracted: '{}'", t)))
      .onErrorResume(error -> {
        log.error("Title extraction failed: {}", error.getMessage(), error);

        return Mono.just(Optional.empty());
      });
  }

  private Mono<Optional<TagsDto>> extractTags(@NonNull String content) {
    return Mono.fromCallable(() -> {
        var result = tagModel.process(content);

        return Optional.ofNullable(result);
      })
      .subscribeOn(Schedulers.boundedElastic())
      .doOnSubscribe(sub -> log.debug("Starting tags extraction"))
      .doOnSuccess(opt -> opt.ifPresent(t -> log.debug("Tags extracted: '{}'", t)))
      .onErrorResume(error -> {
        log.error("Tags extraction failed: {}", error.getMessage(), error);

        return Mono.just(Optional.empty());
      });
  }

  private Mono<Optional<CorrespondentDto>> extractCorrespondent(@NonNull String content) {
    return Mono.fromCallable(() -> {
        var result = correspondentModel.process(content);

        return Optional.ofNullable(result);
      })
      .subscribeOn(Schedulers.boundedElastic())
      .doOnSubscribe(sub -> log.debug("Starting correspondent extraction"))
      .doOnSuccess(opt -> opt.ifPresent(t -> log.debug("Correspondent extracted: '{}'", t)))
      .onErrorResume(error -> {
        log.error("Correspondent extraction failed: {}", error.getMessage(), error);

        return Mono.just(Optional.empty());
      });
  }

  private Mono<Optional<CustomFieldsDto>> extractCustomFields(@NonNull String content) {
    return Mono.fromCallable(() -> {
        var result = customFieldModel.process(content);

        return Optional.ofNullable(result);
      })
      .subscribeOn(Schedulers.boundedElastic())
      .doOnSubscribe(sub -> log.debug("Starting custom fields extraction"))
      .doOnSuccess(opt -> opt.ifPresent(t -> log.debug("Custom fields extracted: '{}'", t)))
      .onErrorResume(error -> {
        log.error("Custom fields extraction failed: {}", error.getMessage(), error);

        return Mono.just(Optional.empty());
      });
  }
}
