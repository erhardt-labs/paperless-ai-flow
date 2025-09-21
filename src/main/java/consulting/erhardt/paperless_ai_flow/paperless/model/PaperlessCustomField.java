package consulting.erhardt.paperless_ai_flow.paperless.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Paperless-ngx custom field definition model
 */
@Value
@Builder
@Jacksonized
public class PaperlessCustomField {
    
    Long id;
    
    String name;
    
    @JsonProperty("data_type")
    String dataType;
    
    @JsonProperty("extra_data")
    String extraData;
    
    @JsonProperty("created")
    String created;
    
    @JsonProperty("modified")
    String modified;
}
