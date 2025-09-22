package consulting.erhardt.paperless_ai_flow.paperless_ngx.client.mappers;

import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Tag;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities.TagResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TagMapper {
  @Mapping(target = "id", source = "id")
  @Mapping(target = "slug", source = "slug")
  @Mapping(target = "name", source = "name")
  @Mapping(target = "color", source = "color")
  @Mapping(target = "textColor", source = "textColor")
  Tag toDto(TagResponse tagResponse);
}
