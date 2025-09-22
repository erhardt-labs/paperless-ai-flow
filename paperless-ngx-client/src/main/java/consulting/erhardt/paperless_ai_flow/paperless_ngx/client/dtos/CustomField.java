package consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@SuperBuilder
@Value
@EqualsAndHashCode(callSuper = true)
public class CustomField extends BaseEntity {
  String name;
  String dataType;
  Map<String, Object> extraData;
}
