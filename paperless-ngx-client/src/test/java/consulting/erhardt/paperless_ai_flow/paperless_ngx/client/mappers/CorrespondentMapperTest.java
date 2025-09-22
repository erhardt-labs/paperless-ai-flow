package consulting.erhardt.paperless_ai_flow.paperless_ngx.client.mappers;

import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities.CorrespondentResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CorrespondentMapperTest {

  private final CorrespondentMapper correspondentMapper = Mappers.getMapper(CorrespondentMapper.class);

  @Test
  @DisplayName("Should map CorrespondentResponse to Correspondent DTO correctly")
  void shouldMapCorrespondentResponseToCorrespondentDto() {
    // Given
    var correspondentResponse = CorrespondentResponse.builder()
      .id(123)
      .slug("example-correspondent")
      .name("Example Correspondent")
      .build();

    // When
    var correspondent = correspondentMapper.toDto(correspondentResponse);

    // Then
    assertNotNull(correspondent, "Mapped Correspondent should not be null");
    assertEquals(123, correspondent.getId(), "Correspondent ID should match");
    assertEquals("example-correspondent", correspondent.getSlug(), "Correspondent slug should match");
    assertEquals("Example Correspondent", correspondent.getName(), "Correspondent name should match");
  }
}
