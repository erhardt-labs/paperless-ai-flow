package consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Jacksonized
@Builder
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class PagedResponse<T> {
  @JsonProperty("count")
  Integer count;

  @JsonProperty("next")
  String next;

  @JsonProperty("previous")
  String previous;

  @JsonProperty("results")
  List<T> results;
}
