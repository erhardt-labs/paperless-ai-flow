package consulting.erhardt.paperless_ai_flow.paperless.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

/**
 * Paperless-ngx custom field definition model
 */
@Value
@Builder
@Jacksonized
public class PaperlessCustomField {

    @JsonProperty("id")
    Long id;

    @JsonProperty("name")
    String name;
    
    @JsonProperty("data_type")
    String dataType;
    
    @JsonProperty("extra_data")
    Map<String, Object> extraData;
    
    @JsonProperty("document_count")
    Integer documentCount;
}
