package consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@Jacksonized
@Builder
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomFieldResponse {
  @NonNull
  @JsonProperty("id")
  Integer id;

  @JsonProperty("name")
  String name;

  @JsonProperty("data_type")
  String dataType;

  @JsonProperty("extra_data")
  Map<String, Object> extraData;
}
