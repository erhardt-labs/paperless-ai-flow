package consulting.erhardt.paperless_ai_flow.app.ai.dtos;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Builder
@Value
public class DocumentMetadataDto {
  String title;

  @NonNull
  @Builder.Default
  List<Integer> tagIds = List.of();

  Integer correspondentId;

  @NonNull
  @Builder.Default
  Map<Integer, String> customFields = Map.of();

  public @NonNull Optional<String> getTitle() {
    return Optional.ofNullable(title);
  }

  public @NonNull Optional<Integer> getCorrespondentId() {
    return Optional.ofNullable(correspondentId);
  }
}
