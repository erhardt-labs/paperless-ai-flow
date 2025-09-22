package consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@Builder
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class CorrespondentResponse {
  @NonNull
  @JsonProperty("id")
  Integer id;

  @NonNull
  @JsonProperty("slug")
  String slug;

  @NonNull
  @JsonProperty("name")
  String name;
}
