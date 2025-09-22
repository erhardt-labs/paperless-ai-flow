package consulting.erhardt.paperless_ai_flow.paperless_ngx.client.configs;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

  @Bean
  public CacheManager cacheManager() {
    var cacheManager = new CaffeineCacheManager(
      "correspondentCache:all",
      "correspondentCache:byId",
      "customFieldsCache:all",
      "customFieldsCache:byId",
      "tagsCache:all",
      "tagsCache:byId"
    );
    cacheManager.setCaffeine(
      Caffeine.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .maximumSize(500)
        .recordStats()
    );

    return cacheManager;
  }
}

