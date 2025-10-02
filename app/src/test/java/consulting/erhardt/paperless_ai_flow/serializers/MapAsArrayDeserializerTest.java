package consulting.erhardt.paperless_ai_flow.serializers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MapAsArrayDeserializer verifying deserialization of custom field arrays.
 */
class MapAsArrayDeserializerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Value
  @Builder
  @com.fasterxml.jackson.databind.annotation.JsonDeserialize(builder = TestDto.TestDtoBuilder.class)
  private static class TestDto {
    @JsonDeserialize(using = MapAsArrayDeserializer.class)
    Map<Integer, String> customFields;

    @com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder(withPrefix = "")
    public static class TestDtoBuilder {
    }
  }

  @Test
  @DisplayName("Should deserialize valid custom fields array")
  void deserialize_validArray_returnsMap() throws JsonProcessingException {
    // Arrange
    var json = """
      {
        "customFields": [
          {"key": 1, "value": "test1"},
          {"key": 2, "value": "test2"}
        ]
      }
      """;

    // Act
    var result = objectMapper.readValue(json, TestDto.class);

    // Assert
    assertNotNull(result.getCustomFields());
    assertEquals(2, result.getCustomFields().size());
    assertEquals("test1", result.getCustomFields().get(1));
    assertEquals("test2", result.getCustomFields().get(2));
  }

  @Test
  @DisplayName("Should deserialize empty array")
  void deserialize_emptyArray_returnsEmptyMap() throws JsonProcessingException {
    // Arrange
    var json = """
      {
        "customFields": []
      }
      """;

    // Act
    var result = objectMapper.readValue(json, TestDto.class);

    // Assert
    assertNotNull(result.getCustomFields());
    assertTrue(result.getCustomFields().isEmpty());
  }

  @Test
  @DisplayName("Should preserve insertion order")
  void deserialize_multipleEntries_preservesOrder() throws JsonProcessingException {
    // Arrange
    var json = """
      {
        "customFields": [
          {"key": 5, "value": "fifth"},
          {"key": 1, "value": "first"},
          {"key": 3, "value": "third"}
        ]
      }
      """;

    // Act
    var result = objectMapper.readValue(json, TestDto.class);

    // Assert
    assertNotNull(result.getCustomFields());
    assertEquals(3, result.getCustomFields().size());
    var keys = result.getCustomFields().keySet().toArray(new Integer[0]);
    assertEquals(5, keys[0], "First key should be 5");
    assertEquals(1, keys[1], "Second key should be 1");
    assertEquals(3, keys[2], "Third key should be 3");
  }

  @Test
  @DisplayName("Should throw exception when input is not an array")
  void deserialize_notAnArray_throwsException() {
    // Arrange
    var json = """
      {
        "customFields": {"key": 1, "value": "test"}
      }
      """;

    // Act & Assert
    var exception = assertThrows(JsonProcessingException.class, () ->
      objectMapper.readValue(json, TestDto.class)
    );
    assertTrue(exception.getMessage().contains("Expected an array"));
  }

  @Test
  @DisplayName("Should throw exception when key is missing")
  void deserialize_missingKey_throwsException() {
    // Arrange
    var json = """
      {
        "customFields": [
          {"value": "test"}
        ]
      }
      """;

    // Act & Assert
    var exception = assertThrows(JsonProcessingException.class, () ->
      objectMapper.readValue(json, TestDto.class)
    );
    assertTrue(exception.getMessage().contains("must have 'key' and 'value'"));
  }

  @Test
  @DisplayName("Should throw exception when value is missing")
  void deserialize_missingValue_throwsException() {
    // Arrange
    var json = """
      {
        "customFields": [
          {"key": 1}
        ]
      }
      """;

    // Act & Assert
    var exception = assertThrows(JsonProcessingException.class, () ->
      objectMapper.readValue(json, TestDto.class)
    );
    assertTrue(exception.getMessage().contains("must have 'key' and 'value'"));
  }

  @Test
  @DisplayName("Should handle string values with special characters")
  void deserialize_specialCharactersInValue_deserializesCorrectly() throws JsonProcessingException {
    // Arrange
    var json = """
      {
        "customFields": [
          {"key": 1, "value": "test with spaces"},
          {"key": 2, "value": "test-with-dashes"},
          {"key": 3, "value": "test@with#special$chars"}
        ]
      }
      """;

    // Act
    var result = objectMapper.readValue(json, TestDto.class);

    // Assert
    assertEquals("test with spaces", result.getCustomFields().get(1));
    assertEquals("test-with-dashes", result.getCustomFields().get(2));
    assertEquals("test@with#special$chars", result.getCustomFields().get(3));
  }

  @Test
  @DisplayName("Should handle large key values")
  void deserialize_largeKeyValues_deserializesCorrectly() throws JsonProcessingException {
    // Arrange
    var json = """
      {
        "customFields": [
          {"key": 999999, "value": "large key"}
        ]
      }
      """;

    // Act
    var result = objectMapper.readValue(json, TestDto.class);

    // Assert
    assertEquals("large key", result.getCustomFields().get(999999));
  }

  @Test
  @DisplayName("Should handle empty string values")
  void deserialize_emptyStringValue_deserializesCorrectly() throws JsonProcessingException {
    // Arrange
    var json = """
      {
        "customFields": [
          {"key": 1, "value": ""}
        ]
      }
      """;

    // Act
    var result = objectMapper.readValue(json, TestDto.class);

    // Assert
    assertEquals("", result.getCustomFields().get(1));
  }
}
