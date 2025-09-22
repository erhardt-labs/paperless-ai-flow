package consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Value
@NonFinal
@EqualsAndHashCode
public class BaseEntity {
  @NonNull
  Integer id;
}
