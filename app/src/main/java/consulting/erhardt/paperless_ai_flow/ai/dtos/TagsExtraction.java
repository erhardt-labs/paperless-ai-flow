package consulting.erhardt.paperless_ai_flow.ai.dtos;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Builder
@Value
public class TagsExtraction {
  @NonNull
  @Builder.Default
  List<Integer> tagIds = List.of();
}
