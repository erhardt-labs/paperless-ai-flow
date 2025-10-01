package consulting.erhardt.paperless_ai_flow.ai.ocr;

import consulting.erhardt.paperless_ai_flow.configs.PipelineConfiguration;
import consulting.erhardt.paperless_ai_flow.utils.FileUtils;
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
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcrExtractionModel implements OcrClient {

  private final OpenAiChatModel openAiChatModel;

  public Mono<String> extractText(PipelineConfiguration.PipelineDefinition pipelineDefinition, Media media) {
    return Mono.fromCallable(() -> {
      var model = pipelineDefinition.getOcr().getModel();
      var prompt = Objects.requireNonNullElse(
        pipelineDefinition.getOcr().getPrompt(),
        FileUtils.readFileFromResources("prompts/ocr.txt")
      );

      log.debug("Processing OCR with model {}", model);
      var options = getChatOptions(model);

      return process(media, prompt, options);
    })
    .subscribeOn(Schedulers.boundedElastic());
  }

  @SneakyThrows
  private String process(@NonNull Media media, @NonNull String promptText, @NonNull ChatOptions options) {
    var userMessage = UserMessage.builder()
      .text(promptText)
      .media(media)
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
}
