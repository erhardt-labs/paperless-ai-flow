package consulting.erhardt.paperless_ai_flow.ai.dtos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CorrespondentExtraction DTO.
 */
class CorrespondentExtractionTest {

  @Test
  @DisplayName("Should create CorrespondentExtraction with correspondent ID")
  void builder_withCorrespondentId_createsObject() {
    // Act
    var extraction = CorrespondentExtraction.builder()
      .correspondentId(5)
      .build();

    // Assert
    assertNotNull(extraction);
    assertEquals(5, extraction.getCorrespondentId());
  }

  @Test
  @DisplayName("Should create CorrespondentExtraction with null ID")
  void builder_withNullId_createsObject() {
    // Act
    var extraction = CorrespondentExtraction.builder()
      .correspondentId(null)
      .build();

    // Assert
    assertNotNull(extraction);
    assertNull(extraction.getCorrespondentId());
  }

  @Test
  @DisplayName("Should support equality comparison")
  void equals_sameValues_returnsTrue() {
    // Arrange
    var extraction1 = CorrespondentExtraction.builder()
      .correspondentId(5)
      .build();
    var extraction2 = CorrespondentExtraction.builder()
      .correspondentId(5)
      .build();

    // Assert
    assertEquals(extraction1, extraction2);
    assertEquals(extraction1.hashCode(), extraction2.hashCode());
  }

  @Test
  @DisplayName("Should not be equal with different IDs")
  void equals_differentValues_returnsFalse() {
    // Arrange
    var extraction1 = CorrespondentExtraction.builder()
      .correspondentId(5)
      .build();
    var extraction2 = CorrespondentExtraction.builder()
      .correspondentId(10)
      .build();

    // Assert
    assertNotEquals(extraction1, extraction2);
  }
}
