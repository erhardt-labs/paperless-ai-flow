package consulting.erhardt.paperless_ai_flow.ai.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class TitleExtraction {
  String title;

  @JsonCreator
  public TitleExtraction(@JsonProperty("title") String title) {
    this.title = title;
  }
}
