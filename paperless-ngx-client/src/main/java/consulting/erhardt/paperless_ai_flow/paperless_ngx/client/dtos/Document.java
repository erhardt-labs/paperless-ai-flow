package consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder(toBuilder = true)
@Value
@EqualsAndHashCode(callSuper = true)
public class Document extends BaseEntity {
  String title;
  String content;
  Correspondent correspondent;
  List<Tag> tags;
  List<CustomField> customFields;
}
