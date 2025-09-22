package consulting.erhardt.paperless_ai_flow.paperless_ngx.client.mappers;

import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities.TagResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TagMapperTest {

  private final TagMapper tagMapper = Mappers.getMapper(TagMapper.class);

  @Test
  @DisplayName("Should map TagResponse to Tag DTO correctly")
  void shouldMapTagResponseToTagDto() {
    // Given
    var tagResponse = TagResponse.builder()
      .id(42)
      .slug("test-tag")
      .name("Test Tag")
      .color("#FF5733")
      .textColor("#FFFFFF")
      .build();

    // When
    var tag = tagMapper.toDto(tagResponse);

    // Then
    assertNotNull(tag, "Mapped Tag should not be null");
    assertEquals(42, tag.getId(), "Tag ID should match");
    assertEquals("test-tag", tag.getSlug(), "Tag slug should match");
    assertEquals("Test Tag", tag.getName(), "Tag name should match");
    assertEquals("#FF5733", tag.getColor(), "Tag color should match");
    assertEquals("#FFFFFF", tag.getTextColor(), "Tag text color should match");
  }
}
