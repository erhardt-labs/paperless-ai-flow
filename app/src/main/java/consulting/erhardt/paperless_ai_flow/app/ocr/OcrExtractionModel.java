package consulting.erhardt.paperless_ai_flow.app.ocr;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcrExtractionModel implements OcrClient {

  private final OpenAiChatModel openAiChatModel;

  public Mono<String> extractText(@NonNull BufferedImage image, @NonNull String model, @NonNull String prompt) {
    return Mono.fromCallable(() -> {
        log.debug("Processing image with model {}", model);
        var options = getChatOptions(model);

        return process(image, prompt, options);
      })
      .subscribeOn(Schedulers.boundedElastic());
  }

  @SneakyThrows
  private String process(@NonNull BufferedImage image, @NonNull String promptText, @NonNull ChatOptions options) {
    // Skip OpenAI call if image is empty (prevents API from hanging on blank images)
    if (isImageEmpty(image)) {
      log.debug("Skipping OpenAI call for empty image");
      return "";
    }

    var userMessage = UserMessage.builder()
      .text(promptText)
      .media(convertImageToMedia(image))
      .build();

    var prompt = new Prompt(userMessage, options);
    var response = openAiChatModel.call(prompt);

    return response.getResult().getOutput().getText();
  }

  private ChatOptions getChatOptions(@NonNull String model) {
    return ChatOptions.builder()
      .model(model)
      .build();
  }

  private Media convertImageToMedia(@NonNull BufferedImage image) throws IOException {
    try (var baos = new ByteArrayOutputStream()) {
      var writers = ImageIO.getImageWritersByFormatName("jpeg");
      if (!writers.hasNext()) {
        throw new IllegalStateException("No JPEG image writers found");
      }

      // open new writer
      var writer = writers.next();

      try (var ios = ImageIO.createImageOutputStream(baos)) {
        writer.setOutput(ios);

        var param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
          param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
          param.setCompressionQuality(0.85f);
        }

        writer.write(null, new IIOImage(image, null, null), param);
      } finally {
        writer.dispose();
      }

      var imageBytes = baos.toByteArray();
            /*ImageIO.write(image, "PNG", baos);
            var imageBytes = baos.toByteArray();*/

      var imageResource = new ByteArrayResource(imageBytes) {
        // Some clients like having a filename (optional but nice)
        @Override
        public String getFilename() {
          return "image.png";
        }
      };

      return new Media(MimeTypeUtils.IMAGE_JPEG, imageResource);
    }
  }

  /**
   * Checks if the image is empty (all pixels have the same color).
   * This prevents the OpenAI API from hanging when processing blank images.
   *
   * @param image the BufferedImage to check
   * @return true if the image is empty (all pixels same color), false otherwise
   */
  private boolean isImageEmpty(@NonNull BufferedImage image) {
    if (image.getWidth() == 0 || image.getHeight() == 0) {
      return true;
    }

    // Sample approach: check if all pixels match the first pixel's color
    var firstPixelRgb = image.getRGB(0, 0);

    // For performance, we sample every 10th pixel instead of checking all pixels
    // This is sufficient to detect completely blank images
    var width = image.getWidth();
    var height = image.getHeight();
    var step = Math.max(1, Math.min(width, height) / 10);

    for (var y = 0; y < height; y += step) {
      for (var x = 0; x < width; x += step) {
        if (image.getRGB(x, y) != firstPixelRgb) {
          return false; // Found a different pixel, image is not empty
        }
      }
    }

    return true; // All sampled pixels are the same color
  }
}
