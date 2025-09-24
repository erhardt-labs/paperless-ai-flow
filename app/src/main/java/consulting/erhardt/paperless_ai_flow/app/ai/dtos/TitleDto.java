package consulting.erhardt.paperless_ai_flow.app.ai.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class TitleDto {
  String title;

  @JsonCreator
  public TitleDto(@JsonProperty("title") String title) {
    this.title = title;
  }
}
