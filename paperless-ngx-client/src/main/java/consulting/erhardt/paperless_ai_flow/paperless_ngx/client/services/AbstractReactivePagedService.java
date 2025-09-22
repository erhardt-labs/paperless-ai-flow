package consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services;

import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.BaseEntity;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities.PagedResponse;
import lombok.NonNull;
import org.springframework.cache.CacheManager;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.OptionalInt;

public abstract class AbstractReactivePagedService<RAW, DTO extends BaseEntity> implements PaperlessApiService<DTO> {

  protected final CacheManager cacheManager;

  protected AbstractReactivePagedService(@NonNull CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  protected abstract Mono<PagedResponse<RAW>> fetchPage(int page);

  protected abstract Mono<RAW> fetchById(int id);

  protected Mono<PagedResponse<RAW>> fetchPage(int page, int pageSize) {
    return fetchPage(page);
  }

  protected OptionalInt pageSize() {
    return OptionalInt.empty();
  }

  protected abstract DTO map(RAW raw);

  protected abstract String cacheName();

  protected abstract String cacheByIdName();

  protected String cacheKey() {
    return "all";
  }

  private Flux<RAW> fetchAllRaw() {
    if (pageSize().isPresent()) {
      int ps = pageSize().getAsInt();
      return Flux
        .range(1, Integer.MAX_VALUE)
        .concatMap(p -> fetchPage(p, ps))
        .takeUntil(p -> p.getNext() == null)
        .flatMapIterable(PagedResponse::getResults);
    } else {
      return Flux
        .range(1, Integer.MAX_VALUE)
        .concatMap(this::fetchPage)
        .takeUntil(p -> p.getNext() == null)
        .flatMapIterable(PagedResponse::getResults);
    }
  }

  protected Flux<DTO> getAllFluxNoCache() {
    return fetchAllRaw().map(this::map);
  }

  public Mono<List<DTO>> getAll() {
    return Mono.defer(() -> {
      var cache = cacheManager.getCache(cacheName());
      if (cache != null) {
        var cached = cache.get(cacheKey(), List.class);
        if (cached != null) {
          return Mono.just((List<DTO>) cached);
        }
      }
      return getAllFluxNoCache()
        .collectList()
        .doOnSuccess(list -> {
          if (cache != null && list != null) {
            cache.put(cacheKey(), list);
          }
        });
    });
  }

  public Mono<DTO> getById(@NonNull Integer id) {
    var byIdCache = cacheManager.getCache(cacheByIdName());

    if (byIdCache != null) {
      var wrapper = byIdCache.get(id);
      if (wrapper != null) {
        DTO cached = (DTO) wrapper.get();
        return Mono.just(cached);
      }
    }

    return getAll()
      .flatMap(list -> {
        var fromSnapshot = list.stream().filter(t -> t.getId().equals(id)).findFirst();
        if (fromSnapshot.isPresent()) {
          if (byIdCache != null) {
            byIdCache.put(id, fromSnapshot.get());
          }

          return Mono.just(fromSnapshot.get());
        }

        return fetchById(id)
          .map(this::map)
          .doOnNext(tag -> {
            if (byIdCache != null) {
              byIdCache.put(id, tag);
            }
          });
      });
  }
}
