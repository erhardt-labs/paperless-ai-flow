package consulting.erhardt.paperless_ai_flow.services;

import consulting.erhardt.paperless_ai_flow.configs.PipelineConfiguration.PipelineDefinition;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Document;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services.DocumentService;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.function.Function;

/**
 * Service responsible for polling documents from Paperless-ngx
 * based on pipeline configuration
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentPollingService {

  private final TagService tagService;
  private final DocumentService documentService;

  /**
   * Poll documents for a specific pipeline definition
   */
  public List<Document> pollDocuments(PipelineDefinition pipelineDefinition) {
    var pipelineName = pipelineDefinition.getName();
    var requiredTags = pipelineDefinition.getSelector().getRequiredTags();

    log.debug("Polling documents for pipeline '{}' with required tags: {}",
      pipelineName, requiredTags);

    if (requiredTags.isEmpty()) {
      log.warn("Pipeline '{}' has no required tags configured, skipping polling", pipelineName);
      return List.of();
    }

    try {
      var documents = getDocumentsByTagNames(requiredTags)
        .collectList()
        .block();

      log.info("Polled {} documents for pipeline '{}'", documents != null ? documents.size() : 0, pipelineName);

      return documents != null ? documents : List.of();

    } catch (Exception e) {
      log.error("Failed to poll documents for pipeline '{}': {}", pipelineName, e.getMessage(), e);
      return List.of();
    }
  }

  public Flux<Document> getDocumentsByTagNames(List<String> tagNames) {
    log.debug("Querying documents by tag names: {}", tagNames);

    return tagService.getTagIdsByNames(tagNames)
      .flatMapMany(documentService::getAllByTags)
      .concatMapIterable(Function.identity());
  }
}
