package consulting.erhardt.paperless_ai_flow.ai.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDate;

@Jacksonized
@Builder
@Value
public class CreatedDateExtraction {

  @JsonProperty("created_date")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  LocalDate createdDate;
}
