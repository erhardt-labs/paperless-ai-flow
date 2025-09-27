package consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.serializers.MapAsArraySerializer;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Jacksonized
@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentPatchRequest {
  @JsonProperty("title")
  String title;

  @JsonProperty("created")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  LocalDate created;

  @JsonProperty("content")
  String content;

  @JsonProperty("correspondent")
  Integer correspondentId;

  @JsonProperty("tags")
  List<Integer> tagIds;

  @JsonProperty("custom_fields")
  @JsonSerialize(using = MapAsArraySerializer.class)
  Map<Integer, String> customFields;

  @NonNull
  @JsonProperty("remove_inbox_tags")
  @Builder.Default
  Boolean removeInboxTags = false;
}
