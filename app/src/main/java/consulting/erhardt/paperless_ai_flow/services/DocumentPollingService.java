package consulting.erhardt.paperless_ai_flow.services;

import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Document;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services.DocumentService;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services.TagService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentPollingService {

  private final TagService tagService;
  private final DocumentService documentService;

  public Flux<Document> getDocumentsByTagNames(@NonNull List<String> tagNames) {
    log.debug("Querying documents by tag names: {}", tagNames);

    return tagService.getTagIdsByNames(tagNames)
      .flatMapMany(documentService::getAllByTags)
      .doOnNext(doc -> log.debug("Will enqueue document {} - '{}'", doc.getId(), doc.getTitle()));
  }
}
