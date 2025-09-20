package consulting.erhardt.paperless_ai_flow.config;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.time.Duration;
import java.util.List;

@Value
@Builder
@ConfigurationProperties(prefix = "paperless")
public class PipelineConfiguration {
    
    @NonNull
    ApiConfiguration api;
    
    @NonNull
    @Builder.Default
    List<PipelineDefinition> pipelines = List.of();
    
    @Value
    @Builder
    public static class ApiConfiguration {
        @NonNull
        String baseUrl;
        
        @NonNull
        String token;
    }
    
    @Value
    @Builder
    public static class PipelineDefinition {
        @NonNull
        String name;
        
        @NonNull
        SelectorConfiguration selector;
        
        @NonNull
        @Builder.Default
        PollingConfiguration polling = PollingConfiguration.builder()
                .interval(Duration.ofSeconds(60))
                .enabled(true)
                .build();
        
        @NonNull
        @Builder.Default
        OcrConfiguration ocr = OcrConfiguration.builder().build();
    }
    
    @Value
    @Builder
    public static class SelectorConfiguration {
        @NonNull
        @Builder.Default
        List<String> requiredTags = List.of();
    }
    
    @Value
    @Builder
    public static class PollingConfiguration {
        @NonNull
        @Builder.Default
        Duration interval = Duration.ofSeconds(60);
        
        @Builder.Default
        boolean enabled = true;
    }
    
    @Value
    @Builder
    public static class OcrConfiguration {
        @NonNull
        @Builder.Default
        String model = "openai/gpt-4o";
        
        String prompt;
        
        /**
         * Get the prompt, returning default if not configured
         */
        public String getPrompt() {
            return prompt != null ? prompt : getDefaultPrompt();
        }
        
        private static String getDefaultPrompt() {
            return """
                    Just transcribe the text in this image and preserve the formatting and layout (high quality OCR).
                    Do that for ALL the text in the image. Be thorough and pay attention. This is very important.
                    The image is from a text document so be sure to continue until the bottom of the page.
                    Thanks a lot! You tend to forget about some text in the image so please focus! Use markdown format but without a code block.
                    """;
        }
    }
}
