package consulting.erhardt.paperless_ai_flow.ai.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import consulting.erhardt.paperless_ai_flow.ai.models.TitleExtractionModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.openai.OpenAiChatModel;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TitleExtractionModelTest {

  @Mock
  private OpenAiChatModel openAiChatModel;

  @Mock
  private ObjectMapper objectMapper;

  private TitleExtractionModel titleExtractionModel;

  @BeforeEach
  void setUp() {
    titleExtractionModel = new TitleExtractionModel(openAiChatModel, objectMapper);
  }

  @Test
  void getUserPrompt_shouldReturnPromptWithDocumentContent() {
    // Given
    var documentContent = "Sample invoice document content with important information";

    // When
    var result = titleExtractionModel.getUserPrompt(documentContent);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).contains("### Document content:");
    assertThat(result).contains("```");
    assertThat(result).contains(documentContent);
    assertThat(result).endsWith("```\n");
  }

  @Test
  void getUserPrompt_shouldHandleEmptyContent() {
    // Given
    var documentContent = "";

    // When
    var result = titleExtractionModel.getUserPrompt(documentContent);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).contains("### Document content:");
    assertThat(result).contains("```");
  }

  @Test
  void getUserPrompt_shouldHandleNullContent() {
    // Given
    String documentContent = null;

    // When & Then - Should throw NullPointerException due to @NonNull annotation
    org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> {
      titleExtractionModel.getUserPrompt(documentContent);
    });
  }
}
