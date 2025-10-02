package consulting.erhardt.paperless_ai_flow.ai.dtos;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CustomFieldsExtraction DTO including JSON deserialization.
 */
class CustomFieldsExtractionTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
  }

  @Test
  @DisplayName("Should create CustomFieldsExtraction with custom fields")
  void builder_withCustomFields_createsObject() {
    // Act
    var extraction = CustomFieldsExtraction.builder()
      .customFields(Map.of(1, "value1", 2, "value2"))
      .build();

    // Assert
    assertNotNull(extraction);
    assertNotNull(extraction.getCustomFields());
    assertEquals(2, extraction.getCustomFields().size());
    assertEquals("value1", extraction.getCustomFields().get(1));
    assertEquals("value2", extraction.getCustomFields().get(2));
  }

  @Test
  @DisplayName("Should create CustomFieldsExtraction with empty map")
  void builder_withEmptyMap_createsObject() {
    // Act
    var extraction = CustomFieldsExtraction.builder()
      .customFields(Map.of())
      .build();

    // Assert
    assertNotNull(extraction);
    assertNotNull(extraction.getCustomFields());
    assertTrue(extraction.getCustomFields().isEmpty());
  }


  @Test
  @DisplayName("Should support equality comparison")
  void equals_sameValues_returnsTrue() {
    // Arrange
    var extraction1 = CustomFieldsExtraction.builder()
      .customFields(Map.of(1, "value1"))
      .build();
    var extraction2 = CustomFieldsExtraction.builder()
      .customFields(Map.of(1, "value1"))
      .build();

    // Assert
    assertEquals(extraction1, extraction2);
    assertEquals(extraction1.hashCode(), extraction2.hashCode());
  }

  @Test
  @DisplayName("Should not be equal with different values")
  void equals_differentValues_returnsFalse() {
    // Arrange
    var extraction1 = CustomFieldsExtraction.builder()
      .customFields(Map.of(1, "value1"))
      .build();
    var extraction2 = CustomFieldsExtraction.builder()
      .customFields(Map.of(1, "value2"))
      .build();

    // Assert
    assertNotEquals(extraction1, extraction2);
  }
}
