package consulting.erhardt.paperless_ai_flow.ai.dtos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CreatedDateExtraction DTO including JSON serialization/deserialization.
 */
class CreatedDateExtractionTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
  }

  @Test
  @DisplayName("Should create CreatedDateExtraction with date")
  void builder_withDate_createsObject() {
    // Act
    var extraction = CreatedDateExtraction.builder()
      .createdDate(LocalDate.of(2025, 1, 15))
      .build();

    // Assert
    assertNotNull(extraction);
    assertEquals(LocalDate.of(2025, 1, 15), extraction.getCreatedDate());
  }

  @Test
  @DisplayName("Should create CreatedDateExtraction with null date")
  void builder_withNullDate_createsObject() {
    // Act
    var extraction = CreatedDateExtraction.builder()
      .createdDate(null)
      .build();

    // Assert
    assertNotNull(extraction);
    assertNull(extraction.getCreatedDate());
  }

  @Test
  @DisplayName("Should deserialize from JSON with yyyy-MM-dd format")
  void deserialize_validJson_createsObject() throws Exception {
    // Arrange
    var json = "{\"created_date\": \"2025-01-15\"}";

    // Act
    var extraction = objectMapper.readValue(json, CreatedDateExtraction.class);

    // Assert
    assertNotNull(extraction);
    assertEquals(LocalDate.of(2025, 1, 15), extraction.getCreatedDate());
  }

  @Test
  @DisplayName("Should deserialize from JSON with null date")
  void deserialize_nullDate_createsObjectWithNull() throws Exception {
    // Arrange
    var json = "{\"created_date\": null}";

    // Act
    var extraction = objectMapper.readValue(json, CreatedDateExtraction.class);

    // Assert
    assertNotNull(extraction);
    assertNull(extraction.getCreatedDate());
  }

  @Test
  @DisplayName("Should serialize to JSON with yyyy-MM-dd format")
  void serialize_validDate_producesCorrectJson() throws Exception {
    // Arrange
    var extraction = CreatedDateExtraction.builder()
      .createdDate(LocalDate.of(2025, 1, 15))
      .build();

    // Act
    var json = objectMapper.writeValueAsString(extraction);

    // Assert
    assertTrue(json.contains("\"created_date\":\"2025-01-15\""));
  }

  @Test
  @DisplayName("Should support equality comparison")
  void equals_sameValues_returnsTrue() {
    // Arrange
    var extraction1 = CreatedDateExtraction.builder()
      .createdDate(LocalDate.of(2025, 1, 15))
      .build();
    var extraction2 = CreatedDateExtraction.builder()
      .createdDate(LocalDate.of(2025, 1, 15))
      .build();

    // Assert
    assertEquals(extraction1, extraction2);
    assertEquals(extraction1.hashCode(), extraction2.hashCode());
  }
}
