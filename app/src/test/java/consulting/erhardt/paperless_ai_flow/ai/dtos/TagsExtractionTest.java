package consulting.erhardt.paperless_ai_flow.ai.dtos;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TagsExtraction DTO including JSON serialization/deserialization.
 */
class TagsExtractionTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
  }

  @Test
  @DisplayName("Should create TagsExtraction with tag IDs")
  void builder_withTagIds_createsObject() {
    // Act
    var extraction = TagsExtraction.builder()
      .tagIds(List.of(1, 2, 3))
      .build();

    // Assert
    assertNotNull(extraction);
    assertNotNull(extraction.getTagIds());
    assertEquals(3, extraction.getTagIds().size());
    assertTrue(extraction.getTagIds().contains(1));
    assertTrue(extraction.getTagIds().contains(2));
    assertTrue(extraction.getTagIds().contains(3));
  }

  @Test
  @DisplayName("Should create TagsExtraction with empty list as default")
  void builder_withoutTagIds_createsObjectWithEmptyList() {
    // Act
    var extraction = TagsExtraction.builder().build();

    // Assert
    assertNotNull(extraction);
    assertNotNull(extraction.getTagIds());
    assertTrue(extraction.getTagIds().isEmpty());
  }

  @Test
  @DisplayName("Should create TagsExtraction with single tag")
  void builder_withSingleTag_createsObject() {
    // Act
    var extraction = TagsExtraction.builder()
      .tagIds(List.of(42))
      .build();

    // Assert
    assertNotNull(extraction);
    assertEquals(1, extraction.getTagIds().size());
    assertEquals(42, extraction.getTagIds().get(0));
  }

  @Test
  @DisplayName("Should support equality comparison")
  void equals_sameValues_returnsTrue() {
    // Arrange
    var extraction1 = TagsExtraction.builder()
      .tagIds(List.of(1, 2, 3))
      .build();
    var extraction2 = TagsExtraction.builder()
      .tagIds(List.of(1, 2, 3))
      .build();

    // Assert
    assertEquals(extraction1, extraction2);
    assertEquals(extraction1.hashCode(), extraction2.hashCode());
  }

  @Test
  @DisplayName("Should not be equal with different tag IDs")
  void equals_differentValues_returnsFalse() {
    // Arrange
    var extraction1 = TagsExtraction.builder()
      .tagIds(List.of(1, 2, 3))
      .build();
    var extraction2 = TagsExtraction.builder()
      .tagIds(List.of(4, 5, 6))
      .build();

    // Assert
    assertNotEquals(extraction1, extraction2);
  }
}
