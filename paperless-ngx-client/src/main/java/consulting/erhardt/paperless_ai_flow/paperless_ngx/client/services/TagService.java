package consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services;

import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.PaperlessNgxApiClient;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Tag;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities.PagedResponse;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities.TagResponse;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.mappers.TagMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TagService extends AbstractReactivePagedService<TagResponse, Tag> {

  private static final String CACHE_ALL = "tagsCache:all";
  private static final String CACHE_BYID = "tagsCache:byId";

  private final PaperlessNgxApiClient webClient;
  private final TagMapper mapper;

  public TagService(
    @NonNull PaperlessNgxApiClient webClient,
    @NonNull TagMapper mapper,
    @NonNull CacheManager cacheManager
  ) {
    super(cacheManager);
    this.webClient = webClient;
    this.mapper = mapper;
  }

  public Mono<List<Integer>> getTagIdsByNames(List<String> tagNames) {
    log.debug("Resolving tag names to IDs: {}", tagNames);

    return getAll()
      .flatMapMany(Flux::fromIterable)
      .filter(tag -> tagNames.contains(tag.getName()))
      .map(Tag::getId)
      .collect(Collectors.toList())
      .doOnNext(tagIds -> {
        if (tagIds.size() != tagNames.size()) {
          log.warn("Could not resolve all tag names. Requested: {}, Found: {}", tagNames.size(), tagIds.size());
        }

        log.debug("Resolved tag names to IDs: {} -> {}", tagNames, tagIds);
      });
  }

  public Mono<Tag> getByName(@NonNull String name) {
    return getAll()
      .flatMapMany(Flux::fromIterable)
      .filter(tag -> tag.getName().equals(name))
      .next()
      .doOnNext(tag -> log.debug("Resolved tag name '{}' to ID: {}", name, tag.getId()))
      .switchIfEmpty(Mono.fromRunnable(() ->
        log.warn("Could not resolve tag name '{}'", name)
      ).then(Mono.empty()));
  }

  @Override
  protected Mono<PagedResponse<TagResponse>> fetchPage(int page) {
    log.debug("Fetching tags page={}", page);
    return webClient.getTagsByPage(page);
  }

  @Override
  protected Mono<TagResponse> fetchById(int id) {
    log.debug("Fetching tag by id={}", id);
    return webClient.getTag(id);
  }

  @Override
  protected Tag map(TagResponse raw) {
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
