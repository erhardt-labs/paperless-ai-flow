package consulting.erhardt.paperless_ai_flow.paperless_ngx.client.mappers;

import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Correspondent;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities.CorrespondentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CorrespondentMapper {
  @Mapping(target = "id", source = "id")
  @Mapping(target = "slug", source = "slug")
  @Mapping(target = "name", source = "name")
  Correspondent toDto(CorrespondentResponse correspondentResponse);
}
