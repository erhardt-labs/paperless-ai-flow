package consulting.erhardt.paperless_ai_flow.app.ocr;

import reactor.core.publisher.Mono;

import java.awt.image.BufferedImage;

/**
 * Interface for OCR clients that can process images and return markdown text
 */
public interface OcrClient {

  /**
   * Process an image and return the extracted text as markdown
   *
   * @param image  the image to process
   * @param model  the OCR model to use (e.g., "openai/gpt-4o")
   * @param prompt the prompt to use for OCR instruction
   * @return the extracted text as markdown
   */
  Mono<String> extractText(BufferedImage image, String model, String prompt);
}
