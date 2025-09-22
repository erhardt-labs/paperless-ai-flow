package consulting.erhardt.paperless_ai_flow.paperless_ngx.client.mappers;

import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.CustomField;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities.CustomFieldResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CustomFieldMapper {
  @Mapping(target = "id", source = "id")
  @Mapping(target = "name", source = "name")
  @Mapping(target = "dataType", source = "dataType")
  @Mapping(target = "extraData", source = "extraData")
  CustomField toDto(CustomFieldResponse tagResponse);
}
