package consulting.erhardt.paperless_ai_flow.paperless.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/**
 * Generic API response wrapper for Paperless-ngx paginated results
 */
@Value
@Builder
@Jacksonized
public class PaperlessApiResponse<T> {
    
    Integer count;
    
    String next;
    
    String previous;
    
    List<T> results;
}
