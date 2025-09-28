package consulting.erhardt.paperless_ai_flow.services;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
public final class IdLockRegistryService<K> {

  private final Set<K> locked = ConcurrentHashMap.newKeySet();

  public boolean tryLock(@NonNull K id) {
    var acquired = locked.add(id);
    if (acquired) {
      log.trace("Lock acquired for id={}", id);
    } else {
      log.trace("Lock already held for id={}", id);
    }
    return acquired;
  }

  public void unlock(@NonNull K id) {
    if (locked.remove(id)) {
      log.trace("Lock released for id={}", id);
    } else {
      log.trace("Unlock called but id was not locked: id={}", id);
    }
  }

  public boolean isLocked(@NonNull K id) {
    return locked.contains(id);
  }
}
