package consulting.erhardt.paperless_ai_flow.paperless.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Paperless-ngx correspondent model
 */
@Value
@Builder
@Jacksonized
public class PaperlessCorrespondent {
    
    Long id;
    
    String name;
    
    @JsonProperty("is_insensitive")
    Boolean isInsensitive;
    
    String match;
    
    @JsonProperty("matching_algorithm")
    Integer matchingAlgorithm;
    
    @JsonProperty("document_count")
    Integer documentCount;
    
    @JsonProperty("last_correspondence")
    String lastCorrespondence;
}
