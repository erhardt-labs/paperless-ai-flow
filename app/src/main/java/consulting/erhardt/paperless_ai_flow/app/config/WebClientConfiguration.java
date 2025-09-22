package consulting.erhardt.paperless_ai_flow.app.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for WebClient used to call Paperless-ngx API
 */
@Configuration
@RequiredArgsConstructor
public class WebClientConfiguration {

  private final PipelineConfiguration pipelineConfig;

  @Bean
  public WebClient webClient(WebClient.Builder webClientBuilder) {
    return webClientBuilder
      .baseUrl(pipelineConfig.getApi().getBaseUrl())
      .build();
  }
}
