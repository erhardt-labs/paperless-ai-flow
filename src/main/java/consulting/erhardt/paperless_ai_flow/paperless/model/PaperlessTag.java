package consulting.erhardt.paperless_ai_flow.paperless.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Paperless-ngx tag model
 */
@Value
@Builder
@Jacksonized
public class PaperlessTag {
    
    Long id;
    
    String name;
    
    String slug;
    
    String color;
    
    @JsonProperty("text_color")
    String textColor;
    
    Boolean match;
    
    @JsonProperty("matching_algorithm")
    String matchingAlgorithm;
    
    @JsonProperty("is_insensitive")
    Boolean isInsensitive;
    
    @JsonProperty("is_inbox_tag")
    Boolean isInboxTag;
    
    @JsonProperty("document_count")
    Integer documentCount;
    
    Long owner;
    
    @JsonProperty("user_can_change")
    Boolean userCanChange;
}
