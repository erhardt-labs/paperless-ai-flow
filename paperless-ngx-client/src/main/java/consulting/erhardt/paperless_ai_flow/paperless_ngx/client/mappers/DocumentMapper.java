package consulting.erhardt.paperless_ai_flow.paperless_ngx.client.mappers;

import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Correspondent;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.CustomField;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Document;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Tag;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities.DocumentResponse;
import lombok.NonNull;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DocumentMapper {
  @Mapping(target = "id", source = "response.id")
  @Mapping(target = "title", source = "response.title")
  @Mapping(target = "content", source = "response.content")
  @Mapping(target = "correspondent", source = "correspondent")
  @Mapping(target = "customFields", source = "customFields")
  @Mapping(target = "tags", source = "tags")
  Document toDto(@NonNull DocumentResponse response, Correspondent correspondent, @NonNull List<CustomField> customFields, @NonNull List<Tag> tags);
}
