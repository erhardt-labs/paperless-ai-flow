package consulting.erhardt.paperless_ai_flow.paperless.client;

import consulting.erhardt.paperless_ai_flow.config.PipelineConfiguration;
import consulting.erhardt.paperless_ai_flow.paperless.model.PaperlessApiResponse;
import consulting.erhardt.paperless_ai_flow.paperless.model.PaperlessCorrespondent;
import consulting.erhardt.paperless_ai_flow.paperless.model.PaperlessCustomField;
import consulting.erhardt.paperless_ai_flow.paperless.model.PaperlessDocument;
import consulting.erhardt.paperless_ai_flow.paperless.model.PaperlessTag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Client for interacting with Paperless-ngx REST API
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaperlessApiClient {
    
    private final WebClient webClient;
    private final PipelineConfiguration config;
    
    /**
     * Get all tags from Paperless
     */
    public Flux<PaperlessTag> getTags() {
        log.debug("Fetching all tags from Paperless API");
        
        return webClient.get()
                .uri("/api/tags/")
                .header("Authorization", "Token " + config.getApi().getToken())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PaperlessApiResponse<PaperlessTag>>() {})
                .doOnNext(response -> log.debug("Fetched {} tags", response.getCount()))
                .flatMapMany(response -> Flux.fromIterable(response.getResults()));
    }
    
    /**
     * Get tag IDs by tag names
     */
    public Mono<List<Long>> getTagIdsByNames(List<String> tagNames) {
        log.debug("Resolving tag names to IDs: {}", tagNames);
        
        return getTags()
                .filter(tag -> tagNames.contains(tag.getName()))
                .map(PaperlessTag::getId)
                .collect(Collectors.toList())
                .doOnNext(tagIds -> {
                    if (tagIds.size() != tagNames.size()) {
                        log.warn("Could not resolve all tag names. Requested: {}, Found: {}", 
                                tagNames.size(), tagIds.size());
                    }
                    log.debug("Resolved tag names to IDs: {} -> {}", tagNames, tagIds);
                });
    }
    
    /**
     * Query documents by tag IDs
     */
    public Flux<PaperlessDocument> getDocumentsByTagIds(List<Long> tagIds) {
        if (tagIds.isEmpty()) {
            log.warn("No tag IDs provided for document query");
            return Flux.empty();
        }
        
        var tagsQuery = tagIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        
        log.debug("Querying documents with tag IDs: {}", tagIds);
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/documents/")
                        .queryParam("tags__id__all", tagsQuery)
                        .queryParam("ordering", "-added")
                        .build())
                .header("Authorization", "Token " + config.getApi().getToken())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PaperlessApiResponse<PaperlessDocument>>() {})
                .doOnNext(response -> log.debug("Found {} documents matching tags {}", 
                        response.getCount(), tagIds))
                .flatMapMany(response -> Flux.fromIterable(response.getResults()));
    }
    
    /**
     * Query documents by tag names (convenience method)
     */
    public Flux<PaperlessDocument> getDocumentsByTagNames(List<String> tagNames) {
        log.debug("Querying documents by tag names: {}", tagNames);
        
        return getTagIdsByNames(tagNames)
                .flatMapMany(this::getDocumentsByTagIds);
    }
    
    /**
     * Get all correspondents from Paperless
     */
    public Flux<PaperlessCorrespondent> getCorrespondents() {
        log.debug("Fetching all correspondents from Paperless API");
        
        return webClient.get()
                .uri("/api/correspondents/")
                .header("Authorization", "Token " + config.getApi().getToken())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PaperlessApiResponse<PaperlessCorrespondent>>() {})
                .doOnNext(response -> log.debug("Fetched {} correspondents", response.getCount()))
                .flatMapMany(response -> Flux.fromIterable(response.getResults()));
    }
    
    /**
     * Get all custom fields from Paperless
     */
    public Flux<PaperlessCustomField> getCustomFields() {
        log.debug("Fetching all custom fields from Paperless API");
        
        return webClient.get()
                .uri("/api/custom_fields/")
                .header("Authorization", "Token " + config.getApi().getToken())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PaperlessApiResponse<PaperlessCustomField>>() {})
                .doOnNext(response -> log.debug("Fetched {} custom fields", response.getCount()))
                .flatMapMany(response -> Flux.fromIterable(response.getResults()));
    }
}
