package consulting.erhardt.paperless_ai_flow.app;

import consulting.erhardt.paperless_ai_flow.app.config.PipelineConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableConfigurationProperties(PipelineConfiguration.class)
@ComponentScan("consulting.erhardt.paperless_ai_flow")
public class PaperlessAiFlowApplication {

  public static void main(String[] args) {
    SpringApplication.run(PaperlessAiFlowApplication.class, args);
  }

}
