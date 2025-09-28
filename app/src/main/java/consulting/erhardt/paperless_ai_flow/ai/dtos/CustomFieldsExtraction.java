package consulting.erhardt.paperless_ai_flow.ai.dtos;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import consulting.erhardt.paperless_ai_flow.serializers.MapAsArrayDeserializer;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.Map;

@Builder
@Value
public class CustomFieldsExtraction {
  @NonNull
  @JsonDeserialize(using = MapAsArrayDeserializer.class)
  Map<Integer, String> customFields;
}
