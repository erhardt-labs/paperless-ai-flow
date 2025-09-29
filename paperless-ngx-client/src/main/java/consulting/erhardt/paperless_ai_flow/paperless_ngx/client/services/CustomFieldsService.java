package consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services;

import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.PaperlessNgxApiClient;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.CustomField;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities.CustomFieldResponse;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities.PagedResponse;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.mappers.CustomFieldMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class CustomFieldsService extends AbstractReactivePagedService<CustomFieldResponse, CustomField> {

  private static final String CACHE_ALL = "customFieldsCache:all";
  private static final String CACHE_BYID = "customFieldsCache:byId";

  private final PaperlessNgxApiClient webClient;
  private final CustomFieldMapper mapper;

  public CustomFieldsService(
    @NonNull PaperlessNgxApiClient webClient,
    @NonNull CustomFieldMapper mapper,
    @NonNull CacheManager cacheManager
  ) {
    super(cacheManager);
    this.webClient = webClient;
    this.mapper = mapper;
  }

  public Mono<CustomField> getByName(@NonNull String name) {
    return getAll()
      .flatMapMany(Flux::fromIterable)
      .filter(customField -> customField.getName().equals(name))
      .next()
      .doOnNext(customField -> log.debug("Resolved custom field name '{}' to ID: {}", name, customField.getId()))
      .switchIfEmpty(Mono.fromRunnable(() ->
        log.warn("Could not resolve custom field name '{}'", name)
      ).then(Mono.empty()));
  }

  @Override
  protected Mono<PagedResponse<CustomFieldResponse>> fetchPage(int page) {
    log.debug("Fetching custom fields page={}", page);
    return webClient.getCustomFieldsByPage(page);
  }

  @Override
  protected Mono<CustomFieldResponse> fetchById(int id) {
    log.debug("Fetching custom field by id={}", id);
    return webClient.getCustomField(id);
  }

  @Override
  protected CustomField map(CustomFieldResponse raw) {
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
