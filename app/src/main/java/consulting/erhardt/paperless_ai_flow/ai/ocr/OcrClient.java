package consulting.erhardt.paperless_ai_flow.ai.ocr;

import consulting.erhardt.paperless_ai_flow.configs.PipelineConfiguration;
import org.springframework.ai.content.Media;
import reactor.core.publisher.Mono;

public interface OcrClient {
  Mono<String> extractText(PipelineConfiguration.PipelineDefinition pipelineDefinition, Media media);
}
