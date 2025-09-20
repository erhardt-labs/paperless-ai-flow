package consulting.erhardt.paperless_ai_flow.service;

import consulting.erhardt.paperless_ai_flow.config.PipelineConfiguration.PipelineDefinition;
import consulting.erhardt.paperless_ai_flow.paperless.client.PaperlessApiClient;
import consulting.erhardt.paperless_ai_flow.paperless.model.PaperlessDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Service responsible for polling documents from Paperless-ngx
 * based on pipeline configuration
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentPollingService {
    
    private final PaperlessApiClient paperlessApiClient;
    
    /**
     * Poll documents for a specific pipeline definition
     */
    public List<PaperlessDocument> pollDocuments(PipelineDefinition pipelineDefinition) {
        var pipelineName = pipelineDefinition.getName();
        var requiredTags = pipelineDefinition.getSelector().getRequiredTags();
        
        log.debug("Polling documents for pipeline '{}' with required tags: {}", 
                pipelineName, requiredTags);
        
        if (requiredTags.isEmpty()) {
            log.warn("Pipeline '{}' has no required tags configured, skipping polling", pipelineName);
            return List.of();
        }
        
        try {
            var documents = paperlessApiClient.getDocumentsByTagNames(requiredTags)
                    .collectList()
                    .block();
            
            log.info("Polled {} documents for pipeline '{}'", 
                    documents != null ? documents.size() : 0, pipelineName);
            
            return documents != null ? documents : List.of();
            
        } catch (Exception e) {
            log.error("Failed to poll documents for pipeline '{}': {}", pipelineName, e.getMessage(), e);
            return List.of();
        }
    }
    
    /**
     * Poll documents reactively for a specific pipeline definition
     */
    public Flux<PaperlessDocument> pollDocumentsReactive(PipelineDefinition pipelineDefinition) {
        var pipelineName = pipelineDefinition.getName();
        var requiredTags = pipelineDefinition.getSelector().getRequiredTags();
        
        log.debug("Reactively polling documents for pipeline '{}' with required tags: {}", 
                pipelineName, requiredTags);
        
        if (requiredTags.isEmpty()) {
            log.warn("Pipeline '{}' has no required tags configured, returning empty flux", pipelineName);
            return Flux.empty();
        }
        
        return paperlessApiClient.getDocumentsByTagNames(requiredTags)
                .doOnNext(document -> log.debug("Found document {} for pipeline '{}'", 
                        document.getId(), pipelineName))
                .doOnError(error -> log.error("Error polling documents for pipeline '{}': {}", 
                        pipelineName, error.getMessage(), error))
                .onErrorResume(error -> {
                    log.warn("Continuing after error in pipeline '{}' polling", pipelineName);
                    return Flux.empty();
                });
    }
}
