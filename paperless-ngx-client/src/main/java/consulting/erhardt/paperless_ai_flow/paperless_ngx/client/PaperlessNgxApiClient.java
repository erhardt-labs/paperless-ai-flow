package consulting.erhardt.paperless_ai_flow.paperless_ngx.client;

import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities.*;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import reactor.core.publisher.Mono;

@HttpExchange("/api")
public interface PaperlessNgxApiClient {

  @GetExchange("/documents/")
  Mono<PagedResponse<DocumentResponse>> getDocumentsByPage(@RequestParam("page") int page);

  @GetExchange("/documents/")
  Mono<PagedResponse<DocumentResponse>> getDocumentsByPage(@RequestParam("page") int page, @RequestParam("tags__id__all") String tagIdsList, @RequestParam("ordering") String ordering);

  @GetExchange("/documents/{id}/")
  Mono<DocumentResponse> getDocument(@PathVariable("id") Integer id);

  @GetExchange("/documents/{id}/download/")
  Mono<byte[]> downloadDocument(@PathVariable("id") Integer id);

  @GetExchange("/correspondents/")
  Mono<PagedResponse<CorrespondentResponse>> getCorrespondentsByPage(@RequestParam("page") Integer page);

  @GetExchange("/correspondents/{id}/")
  Mono<CorrespondentResponse> getCorrespondent(@PathVariable("id") Integer id);

  @GetExchange("/tags/")
  Mono<PagedResponse<TagResponse>> getTagsByPage(@RequestParam("page") Integer page);

  @GetExchange("/tags/{id}/")
  Mono<TagResponse> getTag(@PathVariable("id") Integer id);

  @GetExchange("/custom_fields/")
  Mono<PagedResponse<CustomFieldResponse>> getCustomFieldsByPage(@RequestParam("page") Integer page);

  @GetExchange("/custom_fields/{id}/")
  Mono<CustomFieldResponse> getCustomField(@PathVariable("id") Integer id);
}
