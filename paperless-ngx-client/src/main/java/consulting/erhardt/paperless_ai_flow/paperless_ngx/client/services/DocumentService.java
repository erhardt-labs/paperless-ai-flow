package consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services;

import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.PaperlessNgxApiClient;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Document;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities.DocumentResponse;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities.PagedResponse;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.mappers.DocumentMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService implements PaperlessApiService<Document> {
  private final PaperlessNgxApiClient webClient;
  private final DocumentMapper documentMapper;
  private final CorrespondentService correspondentService;
  private final CustomFieldsService customFieldsService;
  private final TagService tagService;

  @Override
  public Mono<List<Document>> getAll() {
    return Flux
      .range(1, Integer.MAX_VALUE)
      .concatMap(webClient::getDocumentsByPage)
      .takeUntil(p -> p.getNext() == null)
      .flatMapIterable(PagedResponse::getResults)
      .flatMap(this::toDto)
      .collectList();
  }

  public Mono<List<Document>> getAllByTags(@NonNull List<Integer> tagIds) {
    var tagsIdsList = tagIds.stream()
      .map(String::valueOf)
      .collect(Collectors.joining(","));

    return Flux
      .range(1, Integer.MAX_VALUE)
      .concatMap(page -> webClient.getDocumentsByPage(page, tagsIdsList, "-added"))
      .takeUntil(p -> p.getNext() == null)
      .flatMapIterable(PagedResponse::getResults)
      .flatMap(this::toDto)
      .collectList();
  }

  @Override
  public Mono<Document> getById(@NonNull Integer id) {
    return webClient.getDocument(id)
      .flatMap(this::toDto);
  }

  public Mono<byte[]> downloadById(@NonNull Integer id) {
    return webClient.downloadDocument(id);
  }

  private Mono<Document> toDto(DocumentResponse resp) {
    var correspondentMono = Mono.justOrEmpty(resp.getCorrespondentId())
      .flatMap(correspondentService::getById)
      .map(Optional::of)
      .defaultIfEmpty(Optional.empty());

    var customFieldsMono = Mono.justOrEmpty(resp.getCustomFields())
      .flatMapMany(Flux::fromIterable)
      .flatMap((field) -> customFieldsService.getById(field.getId()))
      .collectList();

    var tagsMono = Mono.justOrEmpty(resp.getTagIds())
      .flatMapMany(Flux::fromIterable)
      .flatMap(tagService::getById)
      .collectList();

    return Mono.zip(correspondentMono, customFieldsMono, tagsMono)
      .map(t -> documentMapper.toDto(resp, t.getT1().orElse(null), t.getT2(), t.getT3()));
  }
}
