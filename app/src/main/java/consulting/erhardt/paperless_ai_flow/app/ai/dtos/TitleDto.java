package consulting.erhardt.paperless_ai_flow.app.ai.dtos;

import lombok.NonNull;
import lombok.Value;

@Value
public class TitleDto {
  @NonNull
  String title;
}
