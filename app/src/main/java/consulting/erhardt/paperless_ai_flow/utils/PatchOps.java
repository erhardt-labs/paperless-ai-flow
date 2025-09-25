package consulting.erhardt.paperless_ai_flow.utils;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import reactor.core.publisher.Mono;

import java.util.function.BiFunction;

@UtilityClass
public class PatchOps {
  public <T, M> Mono<M> applyIfPresent(
    @NonNull Mono<M> current,
    @NonNull Mono<T> source,
    @NonNull BiFunction<M, T, M> applier
  ) {
    return current.flatMap(cur -> source
      .map(val -> applier.apply(cur, val))
      .defaultIfEmpty(cur)
    );
  }
}
