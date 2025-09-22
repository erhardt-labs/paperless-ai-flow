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
                // No prompt set - should use default
                .build();
        
        // When: Get the prompt
        var prompt = ocrConfig.getPrompt();
        
        // Then: Should get default prompt, not null
        assertThat(prompt).isNotNull();
        assertThat(prompt).isNotEmpty();
        assertThat(prompt).contains("Just transcribe the text in this image");
        assertThat(prompt).contains("preserve the formatting and layout");
        assertThat(prompt).contains("markdown format but without a code block");
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
        
        // Then: Should have defaults
        assertThat(model).isEqualTo("openai/gpt-4o");
        assertThat(prompt).isNotNull();
        assertThat(prompt).contains("Just transcribe the text");
    }
}
