package consulting.erhardt.paperless_ai_flow.app.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for PipelineConfiguration to ensure default prompt handling
 */
class PipelineConfigurationTest {

    @Test
    void shouldProvideDefaultPromptWhenNotConfigured() {
        // Given: OCR configuration without explicit prompt
        var ocrConfig = PipelineConfiguration.OcrConfiguration.builder()
                .model("openai/gpt-4o")
                // No prompt set - configuration should return null, service handles default
                .build();

        // When: Get the prompt
        var prompt = ocrConfig.getPrompt();

        // Then: Should be null - the PdfOcrService handles loading the default from prompts/ocr.md
        assertThat(prompt).isNull();
    }

    @Test
    void shouldUseConfiguredPromptWhenProvided() {
        // Given: OCR configuration with explicit prompt
        var customPrompt = "Custom OCR prompt for testing";
        var ocrConfig = PipelineConfiguration.OcrConfiguration.builder()
                .model("openai/gpt-4o")
                .prompt(customPrompt)
                .build();

        // When: Get the prompt
        var prompt = ocrConfig.getPrompt();

        // Then: Should use the configured prompt
        assertThat(prompt).isEqualTo(customPrompt);
    }

    @Test
    void shouldProvideDefaultModelWhenNotConfigured() {
        // Given: OCR configuration with defaults
        var ocrConfig = PipelineConfiguration.OcrConfiguration.builder().build();

        // When: Get model and prompt
        var model = ocrConfig.getModel();
        var prompt = ocrConfig.getPrompt();

        // Then: Should have default model, prompt is null (handled by PdfOcrService)
        assertThat(model).isEqualTo("openai/gpt-4o");
        assertThat(prompt).isNull();
    }
}
