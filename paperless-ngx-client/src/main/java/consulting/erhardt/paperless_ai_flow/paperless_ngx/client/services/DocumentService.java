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
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

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

  /*public Mono<List<Document>> getAllByTags(@NonNull List<Integer> tagIds) {
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
  }*/

  public Flux<Document> getAllByTags(@NonNull List<Integer> tagIds) {
    if (tagIds.isEmpty()) {
      return Flux.empty();
    }

    // Start with page 1, then expand to next pages until getNext() == null
    return Mono.defer(() -> fetchPageByTags(1, tagIds))
      .map(firstPage -> Tuples.of(1, firstPage))
      .expand(tuple -> {
        var pageIndex = tuple.getT1();
        var response  = tuple.getT2();

        // Stop expanding when there's no "next" page
        if (response.getNext() == null) {
          return Mono.empty();
        }

        // Otherwise fetch next page (pageIndex + 1)
        var nextPage = pageIndex + 1;
        return fetchPageByTags(nextPage, tagIds).map(nextResp -> Tuples.of(nextPage, nextResp));
      })
      .map(Tuple2::getT2)
      .concatMapIterable(PagedResponse::getResults)
      .concatMap(this::toDto);
  }

  @Override
  public Mono<Document> getById(@NonNull Integer id) {
    return webClient.getDocument(id)
      .flatMap(this::toDto);
  }

  public Mono<Document> patch(@NonNull Document document, boolean removeInboxTags) {
    var request = documentMapper.toPatchRequest(document, removeInboxTags);

    return webClient.patchDocument(document.getId(), request)
      .flatMap(this::toDto);
  }

  public Mono<byte[]> downloadById(@NonNull Integer id) {
    return webClient.downloadDocument(id);
  }

  private Mono<PagedResponse<DocumentResponse>> fetchPageByTags(int page, @NonNull List<Integer> tagIds) {
    var tagsIdsList = tagIds.stream()
      .map(String::valueOf)
      .collect(Collectors.joining(","));

    return webClient.getDocumentsByPage(page, tagsIdsList, "-added");
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
