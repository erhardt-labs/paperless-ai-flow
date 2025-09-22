package consulting.erhardt.paperless_ai_flow.paperless_ngx.client.configs;

import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class TestPaperlessNgxHttpClientConfig extends PaperlessNgxHttpClientConfig {
  public void setPaperlessBaseUrl(String paperlessBaseUrl) {
    this.paperlessBaseUrl = paperlessBaseUrl;
  }
}
