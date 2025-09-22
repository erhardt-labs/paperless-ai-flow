package consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;


@SuperBuilder
@Value
@EqualsAndHashCode(callSuper = true)
public class Tag extends BaseEntity {
  String slug;
  String name;
  String color;
  String textColor;
}
