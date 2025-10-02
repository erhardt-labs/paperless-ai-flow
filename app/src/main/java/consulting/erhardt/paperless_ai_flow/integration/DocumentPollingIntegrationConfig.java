package consulting.erhardt.paperless_ai_flow.integration;

import consulting.erhardt.paperless_ai_flow.configs.PipelineConfiguration;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Correspondent;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Document;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Tag;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services.DocumentService;
import consulting.erhardt.paperless_ai_flow.services.*;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Configuration
@EnableIntegration
@IntegrationComponentScan
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class DocumentPollingIntegrationConfig {

  private final PipelineConfiguration pipelineConfig;
  private final DocumentPollingService pollingService;
  private final PdfOcrService pdfOcrService;
  private final DocumentMetadataExtractionService metadataExtractionService;
  private final DocumentFieldPatchingService documentFieldPatchingService;
  private final DocumentService documentService;

  @Qualifier("documentLockRegistry")
  private final IdLockRegistryService<Integer> documentLockRegistry;

  @Qualifier("pollingChannel")
  private final QueueChannel pollingChannel;

  /**
   * Poll new documents all 30 seconds
   */
  @Scheduled(fixedRate = 30000)
  public void pollDocuments() {
    var enabledPipelines = pipelineConfig.getPipelines().stream()
      .filter(pipeline -> pipeline.getPolling().isEnabled())
      .toList();

    for (var pipeline : enabledPipelines) {
      try {
        var remainingCapacity = pollingChannel.getRemainingCapacity();

        if (remainingCapacity <= 0) {
          log.debug("Polling channel full (remainingCapacity=0), skip for pipeline '{}'", pipeline.getName());
          continue;
        }

        var docsFlux = pollingService.getDocumentsByTagNames(pipeline.getSelector().getRequiredTags());

        var successfullyEnqueuedDocs = docsFlux
          // only allow documents, that are not locked
          .filter(doc -> {
            var locked = documentLockRegistry.tryLock(doc.getId());
            if (!locked) {
              log.debug("Skip locked document {}", doc.getId());
            }
            return locked;
          })
          // try to queue document, if not accepted, unlock document again
          .handle((doc, sink) -> {
            var message = MessageBuilder
              .withPayload(doc)
              .setHeader("pipeline", pipeline)
              .setHeader("pipelineName", pipeline.getName())
              .build();

            var accepted = pollingChannel.send(message, 0); // non-blocking
            if (accepted) {
              log.debug("Enqueued document {} (title: '{}') for pipeline '{}'", doc.getId(), doc.getTitle(), pipeline.getName());

              sink.next(doc);
            } else {
              log.debug("Queue full while enqueuing document {}, unlock and stop filling this cycle", doc.getId());

              documentLockRegistry.unlock(doc.getId());
            }
          })
          .take(remainingCapacity)
          .collectList()
          .block();

        // log enqueued documents
        var enqueued = successfullyEnqueuedDocs != null ? successfullyEnqueuedDocs.size() : 0;
        if (enqueued > 0) {
          log.info("Enqueued {} documents for pipeline '{}'", enqueued, pipeline.getName());
        }
      } catch (Exception e) {
        log.error("Error polling documents for pipeline '{}': {}", pipeline.getName(), e.getMessage(), e);
      }
    }
  }

  /**
   * OCR Service Activator - processes documents from pollingChannel
   */
  @ServiceActivator(inputChannel = "pollingChannel", outputChannel = "metadataExtractChannel")
  public Message<Document> processDocumentOcr(Message<Document> message) {
    var document = message.getPayload();
    var pipelineName = getPipelineName(message);

    log.info("Pipeline '{}': Processing OCR {}", pipelineName, prettyPrintDocument(document));

    try {
      var pipeline = getPipelineDefinition(message);
      var ocrResult = pdfOcrService.processDocument(document, pipeline).block();

      if (ocrResult != null) {
        log.info("OCR completed for document {} from pipeline '{}', result length: {}",
          document.getId(), pipelineName, ocrResult.length());

        // update document
        var updatedDocument = document.toBuilder()
          .content(ocrResult)
          .build();

        // Create result message with original headers plus OCR result
        return MessageBuilder
          .withPayload(updatedDocument)
          .copyHeaders(message.getHeaders())
          .build();
      } else {
        log.error("Error when doing OCR of document '{}' from pipeline '{}'", document.getId(), pipelineName);
      }
    } catch (Exception e) {
      log.error("Error processing OCR for document {} from pipeline '{}': {}", document.getId(), pipelineName, e.getMessage(), e);
    }

    unlockDocument(message);
    return null;
  }

  /**
   * Metadata Extraction Service Activator - processes OCR results from ocrResultChannel
   */
  @ServiceActivator(inputChannel = "metadataExtractChannel", outputChannel = "metadataResultChannel")
  public Message<Document> processMetadataExtraction(Message<Document> message) {
    var document = message.getPayload();
    var pipelineName = getPipelineName(message);

    log.info("Pipeline '{}': Metadata extracting {}", pipelineName, prettyPrintDocument(document));

    try {
      var pipeline = getPipelineDefinition(message);
      var processedDocument = metadataExtractionService.extractMetadata(pipeline, document).block();

      if (processedDocument != null) {
        // Create result message with metadata
        return MessageBuilder
          .withPayload(processedDocument)
          .copyHeaders(message.getHeaders())
          .build();
      } else {
        log.error("Error when processing metadata of document '{}' from pipeline '{}'", document.getId(), pipelineName);
      }
    } catch (Exception e) {
      log.error("Error processing metadata extraction for document {} from pipeline '{}': {}",
        document.getId(), pipelineName, e.getMessage(), e);
    }

    unlockDocument(message);
    return null;
  }

  @ServiceActivator(inputChannel = "metadataResultChannel", outputChannel = "finishedDocumentChannel")
  public Message<Document> processPatching(Message<Document> message) {
    var document = message.getPayload();
    var pipelineName = getPipelineName(message);

    log.info("Pipeline '{}': Field patching {}", pipelineName, prettyPrintDocument(document));

    try {
      var pipeline = getPipelineDefinition(message);
      var patches = pipeline.getPatches();

      if (patches.isEmpty()) {
        log.info("Nothing to patch for document {} from pipeline '{}'", document.getId(), pipelineName);

        return MessageBuilder
          .withPayload(document)
          .copyHeaders(message.getHeaders())
          .build();
      } else {
        var patchedDocument = documentFieldPatchingService.applyPatches(document, pipeline.getPatches()).block();

        if (patchedDocument != null) {
          return MessageBuilder
            .withPayload(patchedDocument)
            .copyHeaders(message.getHeaders())
            .build();
        } else {
          log.error("Error when patching fields of document '{}' from pipeline '{}'", document.getId(), pipelineName);
        }
      }
    } catch (Exception e) {
      log.error("Error processing field patches for document {} from pipeline '{}': {}", document.getId(), pipelineName, e.getMessage(), e);
    }

    unlockDocument(message);
    return null;
  }

  @ServiceActivator(inputChannel = "finishedDocumentChannel")
  public void handleFinishedDocument(Message<Document> message) {
    var document = message.getPayload();
    var pipelineName = getPipelineName(message);

    log.info("Pipeline '{}': Saving {}", pipelineName, prettyPrintDocument(document));

    try {
      var pipeline = getPipelineDefinition(message);

      documentService.patch(document, pipeline.isRemoveInboxTags()).block();

      log.info("Document '{}' from pipeline '{}' has been saved.", document.getId(), pipelineName);
    } catch (Exception e) {
      log.error("Error processing field patches for document {} from pipeline '{}': {}", document.getId(), pipelineName, e.getMessage(), e);
    }

    unlockDocument(message);
  }

  private void unlockDocument(Message<Document> message) {
    var lockedId = message.getPayload().getId();

    documentLockRegistry.unlock(lockedId);
  }

  private @NonNull PipelineConfiguration.PipelineDefinition getPipelineDefinition(@NonNull Message<?> message) {
    if (message.getHeaders().get("pipeline") instanceof PipelineConfiguration.PipelineDefinition pipeline) {
      return pipeline;
    }

    throw new IllegalArgumentException("Missing 'pipeline' header");
  }

  private String getPipelineName(@NonNull Message<?> message) {
    if (message.getHeaders().get("pipelineName") instanceof String pipelineName) {
      return pipelineName;
    }

    return null;
  }

  public String prettyPrintDocument(@NonNull Document doc) {
    var tagsString = Optional.ofNullable(doc.getTags())
      .orElseGet(List::of)
      .stream()
      .map(Tag::getName)
      .collect(Collectors.joining(", "));

    var customFieldsString = Optional.ofNullable(doc.getCustomFields())
      .orElseGet(List::of)
      .stream()
      .map(cf -> cf.getName() + " -> " + cf.getValue())
      .collect(Collectors.joining(", "));

    var correspondentName = Optional.ofNullable(doc.getCorrespondent())
      .map(Correspondent::getName)
      .orElse("n/a");

    var createdDate = Optional.ofNullable(doc.getCreatedDate())
      .map(Object::toString)
      .orElse("n/a");

    return String.format(
      "Document '%d': Title: %s - Correspondent: %s - Created date: %s - Tags: %s - Custom fields: %s",
      doc.getId(),
      Optional.ofNullable(doc.getTitle()).orElse("n/a"),
      correspondentName,
      createdDate,
      tagsString,
      customFieldsString
    );
  }
}
