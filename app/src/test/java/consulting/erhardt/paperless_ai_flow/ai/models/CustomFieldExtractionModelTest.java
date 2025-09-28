package consulting.erhardt.paperless_ai_flow.ai.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import consulting.erhardt.paperless_ai_flow.ai.models.CustomFieldExtractionModel;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.CustomField;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services.CustomFieldsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.openai.OpenAiChatModel;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomFieldExtractionModelTest {

  @Mock
  private OpenAiChatModel openAiChatModel;

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private CustomFieldsService customFieldsService;

  private CustomFieldExtractionModel customFieldExtractionModel;

  @BeforeEach
  void setUp() {
    customFieldExtractionModel = new CustomFieldExtractionModel(
      openAiChatModel,
      objectMapper,
      customFieldsService
    );
  }

  @Test
  void getUserPrompt_shouldReturnPromptWithCustomFieldsAndDocumentContent() {
    // Given
    var documentContent = "Purchase order #12345 dated 2024-01-15";
    var customFields = Arrays.asList(
      CustomField.builder()
        .id(1)
        .name("Purchase Order Number")
        .dataType("string")
        .extraData(new HashMap<>())
        .build(),
      CustomField.builder()
        .id(2)
        .name("Amount")
        .dataType("monetary")
        .extraData(new HashMap<>())
        .build(),
      CustomField.builder()
        .id(3)
        .name("Due Date")
        .dataType("date")
        .extraData(new HashMap<>())
        .build()
    );

    when(customFieldsService.getAll()).thenReturn(Mono.just(customFields));

    // When
    var result = customFieldExtractionModel.getUserPrompt(documentContent);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).contains("### Available custom fields:");
    assertThat(result).contains("- ID: 1, Name: \"Purchase Order Number\", Type: string");
    assertThat(result).contains("- ID: 2, Name: \"Amount\", Type: monetary");
    assertThat(result).contains("- ID: 3, Name: \"Due Date\", Type: date");
    assertThat(result).contains("### Document content:");
    assertThat(result).contains("```");
    assertThat(result).contains(documentContent);
    assertThat(result).endsWith("```\n");
  }

  @Test
  void getUserPrompt_shouldHandleEmptyCustomFieldsList() {
    // Given
    var documentContent = "Sample document content";
    when(customFieldsService.getAll()).thenReturn(Mono.just(Arrays.asList()));

    // When
    var result = customFieldExtractionModel.getUserPrompt(documentContent);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).contains("### Available custom fields:");
    assertThat(result).contains("### Document content:");
    assertThat(result).contains(documentContent);
  }

  @Test
  void getUserPrompt_shouldHandleSingleCustomField() {
    // Given
    var documentContent = "Invoice document";
    var customField = CustomField.builder()
      .id(42)
      .name("Invoice Number")
      .dataType("string")
      .extraData(new HashMap<>())
      .build();

    when(customFieldsService.getAll()).thenReturn(Mono.just(Arrays.asList(customField)));

    // When
    var result = customFieldExtractionModel.getUserPrompt(documentContent);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).contains("### Available custom fields:");
    assertThat(result).contains("- ID: 42, Name: \"Invoice Number\", Type: string");
    assertThat(result).contains("### Document content:");
    assertThat(result).contains(documentContent);
  }

  @Test
  void getUserPrompt_shouldHandleCustomFieldsWithSpecialCharactersInName() {
    // Given
    var documentContent = "Test document";
    var customField = CustomField.builder()
      .id(1)
      .name("Tax & Legal Amount (EUR)")
      .dataType("monetary")
      .extraData(new HashMap<>())
      .build();

    when(customFieldsService.getAll()).thenReturn(Mono.just(Arrays.asList(customField)));

    // When
    var result = customFieldExtractionModel.getUserPrompt(documentContent);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).contains("- ID: 1, Name: \"Tax & Legal Amount (EUR)\", Type: monetary");
  }

  @Test
  void getUserPrompt_shouldHandleDifferentDataTypes() {
    // Given
    var documentContent = "Multi-field document";
    var customFields = Arrays.asList(
      CustomField.builder()
        .id(1)
        .name("Text Field")
        .dataType("string")
        .extraData(new HashMap<>())
        .build(),
      CustomField.builder()
        .id(2)
        .name("Number Field")
        .dataType("int")
        .extraData(new HashMap<>())
        .build(),
      CustomField.builder()
        .id(3)
        .name("Boolean Field")
        .dataType("boolean")
        .extraData(new HashMap<>())
        .build(),
      CustomField.builder()
        .id(4)
        .name("URL Field")
        .dataType("url")
        .extraData(new HashMap<>())
        .build()
    );

    when(customFieldsService.getAll()).thenReturn(Mono.just(customFields));

    // When
    var result = customFieldExtractionModel.getUserPrompt(documentContent);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).contains("Type: string");
    assertThat(result).contains("Type: int");
    assertThat(result).contains("Type: boolean");
    assertThat(result).contains("Type: url");
  }
}
