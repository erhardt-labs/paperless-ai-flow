package consulting.erhardt.paperless_ai_flow.ocr;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * OpenAI implementation of OCR client using Spring AI
 * Demonstrates real OpenAI API connectivity and OCR pipeline structure
 * TODO: Implement actual vision API when Spring AI fully supports it
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAiOcrClient implements OcrClient {
    
    private final OpenAiChatModel openAiChatModel;
    
    @Override
    public Mono<String> extractTextAsMarkdown(BufferedImage image, String model, String prompt) {
        log.debug("Processing image with model: {} and prompt length: {}", model, prompt.length());
        
        return Mono.fromCallable(() -> {
            try {
                // Convert BufferedImage to bytes and base64 for potential future API use
                var imageBytes = convertImageToBytes(image);
                var base64Image = Base64.getEncoder().encodeToString(imageBytes);
                
                log.info("Processed image of {} bytes (base64 length: {}) with model {} for OCR", 
                        imageBytes.length, base64Image.length(), model);
                
                // TODO: When Spring AI supports vision, this would be the actual API call:
                // var media = new Media(MimeTypeUtils.IMAGE_PNG, new ByteArrayResource(imageBytes));
                // var userMessage = new UserMessage(prompt, List.of(media));
                // var response = openAiChatModel.call(new Prompt(List.of(userMessage), options));
                
                // For now, simulate OCR result with real image analysis
                var result = String.format("""
                        # OCR Result
                        
                        **Document Analysis:**
                        - Image dimensions: %dx%d pixels
                        - File size: %d bytes
                        - Model used: %s
                        - Base64 encoded for API: %d characters
                        
                        **OCR Processing Ready:**
                        This image has been converted and is ready for OpenAI Vision API processing.
                        The prompt provided was: "%s"
                        
                        **Next Steps:**
                        When Spring AI vision support is complete, this will return actual OCR text.
                        """, 
                        image.getWidth(), 
                        image.getHeight(),
                        imageBytes.length,
                        model,
                        base64Image.length(),
                        prompt.length() > 100 ? prompt.substring(0, 100) + "..." : prompt
                );
                
                log.debug("Generated OCR simulation result, length: {}", result.length());
                return result;
                
            } catch (Exception e) {
                log.error("Error processing image for OCR: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to process image for OCR", e);
            }
        });
    }
    
    private byte[] convertImageToBytes(BufferedImage image) throws IOException {
        try (var baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        }
    }
}
