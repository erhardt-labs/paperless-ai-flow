package consulting.erhardt.paperless_ai_flow.app.ai.dtos;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Builder
@Value
public class DocumentMetadataDto {
  @NonNull
  String title;

  @NonNull
  List<Long> tagIds;

  Long correspondentId;

  @NonNull
  Map<Long, String> customFields;
}
