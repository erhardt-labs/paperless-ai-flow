package consulting.erhardt.paperless_ai_flow.paperless_ngx.client.mappers;

import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities.CustomFieldResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CustomFieldMapper verifying mapping from CustomFieldResponse to CustomField DTO.
 */
@SpringBootTest
class CustomFieldMapperTest {

  @Autowired
  private CustomFieldMapper customFieldMapper;

  @Test
  @DisplayName("Should map all fields from CustomFieldResponse to CustomField DTO")
  void toDto_allFields_mapsCorrectly() {
    // Arrange
    var response = CustomFieldResponse.builder()
      .id(1)
      .name("Test Field")
      .dataType("string")
      .extraData(Map.of("key1", "value1", "key2", 42))
      .build();

    // Act
    var dto = customFieldMapper.toDto(response);

    // Assert
    assertNotNull(dto);
    assertEquals(1, dto.getId());
    assertEquals("Test Field", dto.getName());
    assertEquals("string", dto.getDataType());
    assertNotNull(dto.getExtraData());
    assertEquals(2, dto.getExtraData().size());
    assertEquals("value1", dto.getExtraData().get("key1"));
    assertEquals(42, dto.getExtraData().get("key2"));
    assertNull(dto.getValue(), "Value should be ignored in mapping");
  }

  @Test
  @DisplayName("Should handle null extraData")
  void toDto_nullExtraData_mapsCorrectly() {
    // Arrange
    var response = CustomFieldResponse.builder()
      .id(2)
      .name("Field Without Extra Data")
      .dataType("integer")
      .extraData(null)
      .build();

    // Act
    var dto = customFieldMapper.toDto(response);

    // Assert
    assertNotNull(dto);
    assertEquals(2, dto.getId());
    assertEquals("Field Without Extra Data", dto.getName());
    assertEquals("integer", dto.getDataType());
    assertNull(dto.getExtraData());
    assertNull(dto.getValue());
  }

  @Test
  @DisplayName("Should handle empty extraData")
  void toDto_emptyExtraData_mapsCorrectly() {
    // Arrange
    var response = CustomFieldResponse.builder()
      .id(3)
      .name("Field With Empty Extra Data")
      .dataType("date")
      .extraData(Map.of())
      .build();

    // Act
    var dto = customFieldMapper.toDto(response);

    // Assert
    assertNotNull(dto);
    assertEquals(3, dto.getId());
    assertEquals("Field With Empty Extra Data", dto.getName());
    assertEquals("date", dto.getDataType());
    assertNotNull(dto.getExtraData());
    assertTrue(dto.getExtraData().isEmpty());
    assertNull(dto.getValue());
  }

  @Test
  @DisplayName("Should map different data types")
  void toDto_differentDataTypes_mapsCorrectly() {
    var dataTypes = new String[]{"string", "integer", "float", "date", "boolean", "url", "monetary", "documentlink"};

    for (var dataType : dataTypes) {
      // Arrange
      var response = CustomFieldResponse.builder()
        .id(10)
        .name("Test Field")
        .dataType(dataType)
        .extraData(Map.of())
        .build();

      // Act
      var dto = customFieldMapper.toDto(response);

      // Assert
      assertNotNull(dto, "DTO should not be null for dataType: " + dataType);
      assertEquals(dataType, dto.getDataType(), "DataType should be mapped correctly for: " + dataType);
    }
  }
}
