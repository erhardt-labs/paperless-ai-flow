package consulting.erhardt.paperless_ai_flow.paperless_ngx.client.configs;

import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.PaperlessNgxApiClient;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Component
public class PaperlessNgxHttpClientConfig {
  @Value("${paperless.api.base-url}")
  String paperlessBaseUrl;

  @Value("${paperless.api.token}")
  String paperlessToken;

  PaperlessNgxApiClient paperlessNgxApiClientInstance;

  @Bean
  public PaperlessNgxApiClient paperlessApiClient() {
    if (paperlessNgxApiClientInstance == null) {
      paperlessNgxApiClientInstance = createPaperlessApiClient(this.paperlessBaseUrl, this.paperlessToken);
    }

    return paperlessNgxApiClientInstance;
  }

  public PaperlessNgxApiClient createPaperlessApiClient(@NonNull String baseUrl, @NonNull String token) {
    var restClient = WebClient.builder()
      .baseUrl(baseUrl)
      .defaultHeader(
        HttpHeaders.AUTHORIZATION,
        "Token " + token
      )
      .build();

    var adapter = WebClientAdapter.create(restClient);
    var factory = HttpServiceProxyFactory.builderFor(adapter).build();

    return factory.createClient(PaperlessNgxApiClient.class);
  }
}
