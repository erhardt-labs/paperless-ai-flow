package consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDate;
import java.util.List;

@Jacksonized
@Builder
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentResponse {
  @NonNull
  @JsonProperty("id")
  Integer id;

  @JsonProperty("title")
  String title;

  @JsonProperty("created_date")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  LocalDate createdDate;

  @JsonProperty("content")
  String content;

  @JsonProperty("correspondent")
  Integer correspondentId;

  @JsonProperty("tags")
  List<Integer> tagIds;

  @JsonProperty("custom_fields")
  List<CustomField> customFields;

  @Jacksonized
  @Builder
  @Value
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class CustomField {
    @JsonProperty("field")
    Integer id;

    @JsonProperty("value")
    String value;
  }
}
