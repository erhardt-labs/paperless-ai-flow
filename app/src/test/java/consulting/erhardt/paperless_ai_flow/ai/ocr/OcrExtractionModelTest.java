package consulting.erhardt.paperless_ai_flow.ai.ocr;

import consulting.erhardt.paperless_ai_flow.configs.PipelineConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class OcrExtractionModelTest {

    @Mock
    private OpenAiChatModel openAiChatModel;

    private OcrExtractionModel ocrExtractionModel;

    @BeforeEach
    void setUp() {
        ocrExtractionModel = new OcrExtractionModel(openAiChatModel);
    }

    @Test
    void shouldCreatePipelineDefinitionWithModel() {
        // Given
        var model = "gpt-4-vision-preview";
        var customPrompt = "Extract text from this image";

        // When
        var pipelineDefinition = createPipelineDefinition(model, customPrompt);

        // Then
        assertThat(pipelineDefinition).isNotNull();
        assertThat(pipelineDefinition.getOcr()).isNotNull();
        assertThat(pipelineDefinition.getOcr().getModel()).isEqualTo(model);
        assertThat(pipelineDefinition.getOcr().getPrompt()).isEqualTo(customPrompt);
    }

    @Test
    void shouldCreatePipelineDefinitionWithNullPrompt() {
        // Given
        var model = "gpt-4o";

        // When
        var pipelineDefinition = createPipelineDefinition(model, null);

        // Then
        assertThat(pipelineDefinition).isNotNull();
        assertThat(pipelineDefinition.getOcr()).isNotNull();
        assertThat(pipelineDefinition.getOcr().getModel()).isEqualTo(model);
        assertThat(pipelineDefinition.getOcr().getPrompt()).isNull();
    }

    @Test
    void shouldCreateTestMedia() {
        // When
        var media = createTestMedia();

        // Then
        assertThat(media).isNotNull();
        assertThat(media.getMimeType()).isEqualTo(MimeTypeUtils.IMAGE_JPEG);
    }

    @Test
    void shouldCreateOcrExtractionModel() {
        // When
        var model = new OcrExtractionModel(openAiChatModel);

        // Then
        assertThat(model).isNotNull();
    }

    @Test
    void shouldRequireNonNullPipelineDefinition() {
        // Given
        var media = createTestMedia();

        // When & Then
        var result = ocrExtractionModel.extractText(null, media);

        // Should fail due to null pipeline definition
        assertThrows(NullPointerException.class, () -> {
            result.block();
        });
    }

    @Test
    void shouldRequireNonNullMedia() {
        // Given
        var pipelineDefinition = createPipelineDefinition("gpt-4o", "test");

        // When & Then
        var result = ocrExtractionModel.extractText(pipelineDefinition, null);

        // Should fail due to null media
        assertThrows(NullPointerException.class, () -> {
            result.block();
        });
    }

    private PipelineConfiguration.PipelineDefinition createPipelineDefinition(String model, String prompt) {
        var ocr = PipelineConfiguration.OcrConfiguration.builder()
                .model(model)
                .prompt(prompt)
                .build();

        var selector = PipelineConfiguration.SelectorConfiguration.builder()
                .build();

        return PipelineConfiguration.PipelineDefinition.builder()
                .name("test-pipeline")
                .selector(selector)
                .ocr(ocr)
                .build();
    }

    private Media createTestMedia() {
        return new Media(MimeTypeUtils.IMAGE_JPEG, new org.springframework.core.io.ByteArrayResource("test-image-data".getBytes()));
    }
}
