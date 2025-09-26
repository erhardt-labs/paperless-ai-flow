package consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services;

import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.PaperlessNgxApiClient;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Correspondent;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.CustomField;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities.CorrespondentResponse;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities.PagedResponse;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.mappers.CorrespondentMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class CorrespondentService extends AbstractReactivePagedService<CorrespondentResponse, Correspondent> {

  private static final String CACHE_ALL = "correspondentCache:all";
  private static final String CACHE_BYID = "correspondentCache:byId";

  private final PaperlessNgxApiClient webClient;
  private final CorrespondentMapper mapper;

  public CorrespondentService(
    @NonNull PaperlessNgxApiClient webClient,
    @NonNull CorrespondentMapper mapper,
    @NonNull CacheManager cacheManager
  ) {
    super(cacheManager);
    this.webClient = webClient;
    this.mapper = mapper;
  }

  public Mono<Correspondent> getByName(@NonNull String name) {
    return getAll()
      .flatMapMany(Flux::fromIterable)
      .filter(correspondent -> correspondent.getName().equals(name))
      .next()
      .doOnNext(correspondent -> log.debug("Resolved correspondent name '{}' to ID: {}", name, correspondent.getId()))
      .switchIfEmpty(Mono.fromRunnable(() ->
        log.warn("Could not resolve correspondent name '{}'", name)
      ).then(Mono.empty()));
  }

  @Override
  protected Mono<PagedResponse<CorrespondentResponse>> fetchPage(int page) {
    log.info("Fetching correspondents page={}", page);
    return webClient.getCorrespondentsByPage(page);
  }

  @Override
  protected Mono<CorrespondentResponse> fetchById(int id) {
    log.info("Fetching correspondent by id={}", id);
    return webClient.getCorrespondent(id);
  }

  @Override
  protected Correspondent map(CorrespondentResponse raw) {
    return mapper.toDto(raw);
  }

  @Override
  protected String cacheName() {
    return CACHE_ALL;
  }

  @Override
  protected String cacheByIdName() {
    return CACHE_BYID;
  }
}
