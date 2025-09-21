package consulting.erhardt.paperless_ai_flow.ai.dtos;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Builder
@Value
public class CorrespondentDto {
    @NonNull
    Long correspondentId;
}
