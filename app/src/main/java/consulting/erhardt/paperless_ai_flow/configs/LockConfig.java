package consulting.erhardt.paperless_ai_flow.configs;

import consulting.erhardt.paperless_ai_flow.app.service.IdLockRegistryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LockConfig {
  @Bean
  public IdLockRegistryService<Integer> documentLockRegistry() {
    return new IdLockRegistryService<>();
  }
}
