package consulting.erhardt.paperless_ai_flow.app.ai.dtos;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Builder
@Value
public class TagsDto {
  @NonNull
  List<Long> tagIds;
}
