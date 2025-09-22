package consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services;

import reactor.core.publisher.Mono;

import java.util.List;

public interface PaperlessApiService<DTO> {
  Mono<List<DTO>> getAll();

  Mono<DTO> getById(Integer id);
}
