package consulting.erhardt.paperless_ai_flow;

import consulting.erhardt.paperless_ai_flow.app.config.PipelineConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(PipelineConfiguration.class)
public class PaperlessAiFlowApplication {

  public static void main(String[] args) {
    SpringApplication.run(PaperlessAiFlowApplication.class, args);
  }
}
