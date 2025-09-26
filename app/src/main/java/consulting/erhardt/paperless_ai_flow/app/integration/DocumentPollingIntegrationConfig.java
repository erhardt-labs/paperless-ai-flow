package consulting.erhardt.paperless_ai_flow.app.integration;

import consulting.erhardt.paperless_ai_flow.app.config.PipelineConfiguration;
import consulting.erhardt.paperless_ai_flow.app.service.*;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Document;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Spring Integration configuration for document processing pipeline
 * Documents flow: Polling -> pollingChannel -> OCR -> ocrResultChannel -> Next Steps
 */
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

  @Qualifier("documentLockRegistry")
  private final IdLockRegistryService<Integer> documentLockRegistry;

  @Qualifier("pollingChannel")
  private final MessageChannel pollingChannel;

  /**
   * Scheduled polling that puts documents into pollingChannel
   */
  @Scheduled(fixedRate = 30000) // Poll every 30 seconds
  public void pollDocuments() {
    var enabledPipelines = pipelineConfig.getPipelines().stream()
      .filter(pipeline -> pipeline.getPolling().isEnabled())
      .toList();

    for (var pipeline : enabledPipelines) {
      try {
        var documents = pollingService.pollDocuments(pipeline);
        if (!documents.isEmpty()) {
          log.info("Found {} documents for pipeline '{}', sending to pollingChannel", documents.size(), pipeline.getName());

          // Send each document to the pollingChannel with pipeline context
          for (var document : documents) {
            var id = document.getId();

            if (!documentLockRegistry.tryLock(id)) {
              log.debug("Skip document {} (title: '{}') - lock already held", id, document.getTitle());
              continue;
            }

            var message = MessageBuilder
              .withPayload(document)
              .setHeader("pipeline", pipeline)
              .setHeader("pipelineName", pipeline.getName())
              .build();

            pollingChannel.send(message);

            log.debug("Sent document {} (title: '{}') to pollingChannel for pipeline '{}'",
              document.getId(), document.getTitle(), pipeline.getName());
          }
        }
      } catch (Exception e) {
        log.error("Error polling documents for pipeline '{}': {}",
          pipeline.getName(), e.getMessage(), e);
      }
    }
  }

  /**
   * OCR Service Activator - processes documents from pollingChannel
   */
  @ServiceActivator(inputChannel = "pollingChannel", outputChannel = "metadataExtractChannel")
  public Message<Document> processDocumentOcr(Message<Document> message) {
    var document = message.getPayload();
    var pipeline = (PipelineConfiguration.PipelineDefinition) message.getHeaders().get("pipeline");
    var pipelineName = (String) message.getHeaders().get("pipelineName");

    log.info("Processing OCR for document {} from pipeline '{}'", document.getId(), pipelineName);

    try {
      // Process the document through OCR
      var ocrResult = pdfOcrService.processDocument(document, pipeline).block();

      if(ocrResult != null) {
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
    var pipeline = (PipelineConfiguration.PipelineDefinition) message.getHeaders().get("pipeline");
    var pipelineName = (String) message.getHeaders().get("pipelineName");

    log.info("Starting metadata extraction for document {} from pipeline '{}'",
      document.getId(), pipelineName);

    try {
      // Process the OCR result through metadata extraction
      var processedDocument = metadataExtractionService.extractMetadata(pipeline, document).block();

      if(processedDocument != null) {
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
    var pipeline = (PipelineConfiguration.PipelineDefinition) message.getHeaders().get("pipeline");
    var pipelineName = (String) message.getHeaders().get("pipelineName");
    var patches =  pipeline.getPatches();

    log.info("Starting field patching for document {} from pipeline '{}'", document.getId(), pipelineName);

    if(patches.isEmpty()) {
      log.info("Nothing to patch for document {} from pipeline '{}'", document.getId(), pipelineName);

      return MessageBuilder
        .withPayload(document)
        .copyHeaders(message.getHeaders())
        .build();
    } else {
      try {
        var patchedDocument = documentFieldPatchingService.applyPatches(document, pipeline.getPatches()).block();

        if(patchedDocument != null) {
          return MessageBuilder
            .withPayload(patchedDocument)
            .copyHeaders(message.getHeaders())
            .build();
        } else {
          log.error("Error when patching fields of document '{}' from pipeline '{}'", document.getId(), pipelineName);
        }
      } catch (Exception e) {
        log.error("Error processing field patches for document {} from pipeline '{}': {}", document.getId(), pipelineName, e.getMessage(), e);
      }
    }

    unlockDocument(message);
    return null;
  }

  @ServiceActivator(inputChannel = "finishedDocumentChannel")
  public void handleFinishedDocument(Message<Document> message) {
    var document = message.getPayload();
    var pipeline = (PipelineConfiguration.PipelineDefinition) message.getHeaders().get("pipeline");
    var pipelineName = (String) message.getHeaders().get("pipelineName");

    unlockDocument(message);
  }

  private void unlockDocument(Message<Document> message) {
    var lockedId = message.getPayload().getId();

    documentLockRegistry.unlock(lockedId);
  }
}
