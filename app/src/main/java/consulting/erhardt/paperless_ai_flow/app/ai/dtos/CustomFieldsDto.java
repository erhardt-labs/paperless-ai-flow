package consulting.erhardt.paperless_ai_flow.app.ai.dtos;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.Map;

@Builder
@Value
public class CustomFieldsDto {
  @NonNull
  Map<Long, String> customFields;
}
