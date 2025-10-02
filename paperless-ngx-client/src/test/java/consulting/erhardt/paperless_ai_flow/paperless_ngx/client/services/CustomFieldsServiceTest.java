package consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services;

import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.PaperlessNgxApiClient;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.CustomField;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities.CustomFieldResponse;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities.PagedResponse;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.mappers.CustomFieldMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

/**
 * Unit tests for CustomFieldsService using Mockito to mock dependencies.
 */
@ExtendWith(MockitoExtension.class)
class CustomFieldsServiceTest {

  @Mock
  private PaperlessNgxApiClient webClient;

  @Mock
  private CustomFieldMapper customFieldMapper;

  @Mock
  private CacheManager cacheManager;

  private CustomFieldsService customFieldsService;

  @BeforeEach
  void setup() {
    customFieldsService = new CustomFieldsService(webClient, customFieldMapper, cacheManager);
  }

  @Test
  @DisplayName("Should get all custom fields")
  void getAll_shouldReturnAllFields() {
    // Arrange
    var field1Response = CustomFieldResponse.builder()
      .id(1)
      .name("Invoice Number")
      .dataType("string")
      .build();

    var field2Response = CustomFieldResponse.builder()
      .id(2)
      .name("Amount")
      .dataType("monetary")
      .build();

    var pagedResponse = PagedResponse.<CustomFieldResponse>builder()
      .count(2)
      .next(null)
      .previous(null)
      .results(List.of(field1Response, field2Response))
      .build();

    when(webClient.getCustomFieldsByPage(1)).thenReturn(Mono.just(pagedResponse));
    when(customFieldMapper.toDto(field1Response)).thenReturn(CustomField.builder()
      .id(1)
      .name("Invoice Number")
      .dataType("string")
      .build());
    when(customFieldMapper.toDto(field2Response)).thenReturn(CustomField.builder()
      .id(2)
      .name("Amount")
      .dataType("monetary")
      .build());

    // Act & Assert
    StepVerifier.create(customFieldsService.getAll())
      .expectNextMatches(fields ->
        fields.size() == 2 &&
        fields.get(0).getName().equals("Invoice Number") &&
        fields.get(1).getName().equals("Amount"))
      .verifyComplete();
  }

  @Test
  @DisplayName("Should handle empty custom fields")
  void getAll_emptyResults_returnsEmptyList() {
    // Arrange
    var emptyPagedResponse = PagedResponse.<CustomFieldResponse>builder()
      .count(0)
      .next(null)
      .previous(null)
      .results(List.of())
      .build();

    when(webClient.getCustomFieldsByPage(1)).thenReturn(Mono.just(emptyPagedResponse));

    // Act & Assert
    StepVerifier.create(customFieldsService.getAll())
      .expectNextMatches(List::isEmpty)
      .verifyComplete();
  }

  @Test
  @DisplayName("Should get custom field by ID")
  void getById_existingField_returnsField() {
    // Arrange
    var fieldResponse = CustomFieldResponse.builder()
      .id(1)
      .name("Invoice Number")
      .dataType("string")
      .extraData(Map.of("required", true))
      .build();

    var pagedResponse = PagedResponse.<CustomFieldResponse>builder()
      .count(1)
      .next(null)
      .previous(null)
      .results(List.of(fieldResponse))
      .build();

    when(webClient.getCustomFieldsByPage(1)).thenReturn(Mono.just(pagedResponse));
    when(customFieldMapper.toDto(fieldResponse)).thenReturn(CustomField.builder()
      .id(1)
      .name("Invoice Number")
      .dataType("string")
      .extraData(Map.of("required", true))
      .build());

    // Act & Assert
    StepVerifier.create(customFieldsService.getById(1))
      .expectNextMatches(field ->
        field.getId().equals(1) &&
        field.getName().equals("Invoice Number") &&
        field.getDataType().equals("string"))
      .verifyComplete();
  }

  @Test
  @DisplayName("Should handle 404 for non-existent field")
  void getById_nonExistentField_returnsError() {
    // Arrange
    var emptyPagedResponse = PagedResponse.<CustomFieldResponse>builder()
      .count(0)
      .next(null)
      .previous(null)
      .results(List.of())
      .build();

    when(webClient.getCustomFieldsByPage(1)).thenReturn(Mono.just(emptyPagedResponse));
    when(webClient.getCustomField(999)).thenReturn(Mono.error(new RuntimeException("Not found")));

    // Act & Assert
    StepVerifier.create(customFieldsService.getById(999))
      .expectError(RuntimeException.class)
      .verify();
  }

  @Test
  @DisplayName("Should handle custom fields with various data types")
  void getAll_variousDataTypes_mapsCorrectly() {
    // Arrange
    var stringFieldResponse = CustomFieldResponse.builder()
      .id(1)
      .name("Text Field")
      .dataType("string")
      .build();

    var integerFieldResponse = CustomFieldResponse.builder()
      .id(2)
      .name("Number Field")
      .dataType("integer")
      .build();

    var dateFieldResponse = CustomFieldResponse.builder()
      .id(3)
      .name("Date Field")
      .dataType("date")
      .build();

    var pagedResponse = PagedResponse.<CustomFieldResponse>builder()
      .count(3)
      .next(null)
      .previous(null)
      .results(List.of(stringFieldResponse, integerFieldResponse, dateFieldResponse))
      .build();

    when(webClient.getCustomFieldsByPage(1)).thenReturn(Mono.just(pagedResponse));
    when(customFieldMapper.toDto(stringFieldResponse)).thenReturn(CustomField.builder()
      .id(1)
      .name("Text Field")
      .dataType("string")
      .build());
    when(customFieldMapper.toDto(integerFieldResponse)).thenReturn(CustomField.builder()
      .id(2)
      .name("Number Field")
      .dataType("integer")
      .build());
    when(customFieldMapper.toDto(dateFieldResponse)).thenReturn(CustomField.builder()
      .id(3)
      .name("Date Field")
      .dataType("date")
      .build());

    // Act & Assert
    StepVerifier.create(customFieldsService.getAll())
      .expectNextMatches(fields ->
        fields.size() == 3 &&
        fields.get(0).getDataType().equals("string") &&
        fields.get(1).getDataType().equals("integer") &&
        fields.get(2).getDataType().equals("date"))
      .verifyComplete();
  }

  @Test
  @DisplayName("Should handle multiple pages")
  void getAll_multiplePages_returnsAllFields() {
    // Arrange
    var field1Response = CustomFieldResponse.builder()
      .id(1)
      .name("Field1")
      .dataType("string")
      .build();

    var field2Response = CustomFieldResponse.builder()
      .id(2)
      .name("Field2")
      .dataType("integer")
      .build();

    var page1Response = PagedResponse.<CustomFieldResponse>builder()
      .count(2)
      .next("http://localhost/api/custom_fields/?page=2")
      .previous(null)
      .results(List.of(field1Response))
      .build();

    var page2Response = PagedResponse.<CustomFieldResponse>builder()
      .count(2)
      .next(null)
      .previous("http://localhost/api/custom_fields/?page=1")
      .results(List.of(field2Response))
      .build();

    when(webClient.getCustomFieldsByPage(1)).thenReturn(Mono.just(page1Response));
    when(webClient.getCustomFieldsByPage(2)).thenReturn(Mono.just(page2Response));
    when(customFieldMapper.toDto(field1Response)).thenReturn(CustomField.builder()
      .id(1)
      .name("Field1")
      .dataType("string")
      .build());
    when(customFieldMapper.toDto(field2Response)).thenReturn(CustomField.builder()
      .id(2)
      .name("Field2")
      .dataType("integer")
      .build());

    // Act & Assert
    StepVerifier.create(customFieldsService.getAll())
      .expectNextMatches(fields ->
        fields.size() == 2 &&
        fields.stream().anyMatch(f -> f.getName().equals("Field1")) &&
        fields.stream().anyMatch(f -> f.getName().equals("Field2")))
      .verifyComplete();
  }

  @Test
  @DisplayName("Should handle custom fields with extra data")
  void getAll_withExtraData_mapsCorrectly() {
    // Arrange
    var fieldResponse = CustomFieldResponse.builder()
      .id(1)
      .name("Invoice Number")
      .dataType("string")
      .extraData(Map.of("required", true, "pattern", "INV-\\d{4}"))
      .build();

    var pagedResponse = PagedResponse.<CustomFieldResponse>builder()
      .count(1)
      .next(null)
      .previous(null)
      .results(List.of(fieldResponse))
      .build();

    when(webClient.getCustomFieldsByPage(1)).thenReturn(Mono.just(pagedResponse));
    when(customFieldMapper.toDto(fieldResponse)).thenReturn(CustomField.builder()
      .id(1)
      .name("Invoice Number")
      .dataType("string")
      .extraData(Map.of("required", true, "pattern", "INV-\\d{4}"))
      .build());

    // Act & Assert
    StepVerifier.create(customFieldsService.getAll())
      .expectNextMatches(fields ->
        fields.size() == 1 &&
        fields.get(0).getExtraData() != null &&
        fields.get(0).getExtraData().containsKey("required") &&
        fields.get(0).getExtraData().containsKey("pattern"))
      .verifyComplete();
  }
}
