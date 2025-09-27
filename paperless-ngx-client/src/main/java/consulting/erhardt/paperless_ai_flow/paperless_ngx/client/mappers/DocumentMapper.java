package consulting.erhardt.paperless_ai_flow.paperless_ngx.client.mappers;

import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Correspondent;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.CustomField;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Document;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Tag;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities.DocumentPatchRequest;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities.DocumentResponse;
import lombok.NonNull;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DocumentMapper {
  @Mapping(target = "id", source = "response.id")
  @Mapping(target = "title", source = "response.title")
  @Mapping(target = "createdDate", source = "response.createdDate")
  @Mapping(target = "content", source = "response.content")
  @Mapping(target = "correspondent", source = "correspondent")
  @Mapping(target = "customFields", source = "customFields")
  @Mapping(target = "tags", source = "tags")
  Document toDto(@NonNull DocumentResponse response, Correspondent correspondent, @NonNull List<CustomField> customFields, @NonNull List<Tag> tags);

  @Mapping(target = "title", source = "title")
  @Mapping(target = "created", source = "createdDate")
  @Mapping(target = "content", source = "content")
  @Mapping(target = "correspondentId", source = "correspondent", qualifiedByName = "mapCorrespondent")
  @Mapping(target = "customFields", source = "customFields", qualifiedByName = "mapCustomFields")
  @Mapping(target = "tagIds", source = "tags", qualifiedByName = "mapTagIds")
  DocumentPatchRequest toPatchRequest(@NonNull Document document);

  @Named("mapCorrespondent")
  default Integer mapCorrespondent(Correspondent correspondent) {
    if (correspondent == null) {
      return null;
    }

    return correspondent.getId();
  }

  @Named("mapCustomFields")
  default Map<Integer, String> mapCustomFields(List<CustomField> customFields) {
    if (customFields == null) {
      return null;
    }

    return customFields.stream()
      .collect(Collectors.toMap(
        CustomField::getId,
        CustomField::getValue,
        (a, b) -> a
      ));
  }

  @Named("mapTagIds")
  default List<Integer> mapTagIds(List<Tag> tags) {
    if (tags == null) {
      return null;
    }

    return tags.stream()
      .map(Tag::getId)
      .toList();
  }
}
