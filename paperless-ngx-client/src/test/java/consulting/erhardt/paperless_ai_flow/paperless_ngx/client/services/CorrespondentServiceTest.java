package consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services;

import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.PaperlessNgxApiClient;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities.CorrespondentResponse;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities.PagedResponse;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.mappers.CorrespondentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorrespondentServiceTest {

  @Mock
  private PaperlessNgxApiClient webClient;

  @Mock
  private CorrespondentMapper correspondentMapper;

  @Mock
  private CacheManager cacheManager;

  private CorrespondentService correspondentService;

  @BeforeEach
  void setUp() {
    correspondentService = new CorrespondentService(webClient, correspondentMapper, cacheManager);
  }

  @Test
  void getById_shouldReturnCorrespondent() {
    // Given
    var correspondentId = 1;
    var correspondentResponse = CorrespondentResponse.builder()
        .id(correspondentId)
        .name("Test Correspondent")
        .slug("test-correspondent")
        .build();

    var pagedResponse = PagedResponse.<CorrespondentResponse>builder()
        .count(1)
        .next(null)
        .previous(null)
        .results(List.of(correspondentResponse))
        .build();

    when(webClient.getCorrespondentsByPage(1)).thenReturn(Mono.just(pagedResponse));
    when(correspondentMapper.toDto(correspondentResponse))
        .thenReturn(consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Correspondent.builder()
            .id(correspondentId)
            .name("Test Correspondent")
            .slug("test-correspondent")
            .build());

    // When & Then
    StepVerifier.create(correspondentService.getById(correspondentId))
        .expectNextMatches(correspondent ->
            correspondent.getId().equals(correspondentId) &&
            "Test Correspondent".equals(correspondent.getName()) &&
            "test-correspondent".equals(correspondent.getSlug()))
        .verifyComplete();
  }

  @Test
  void getAll_shouldReturnAllCorrespondents() {
    // Given
    var correspondent1 = CorrespondentResponse.builder()
        .id(1)
        .name("Correspondent 1")
        .slug("correspondent-1")
        .build();

    var correspondent2 = CorrespondentResponse.builder()
        .id(2)
        .name("Correspondent 2")
        .slug("correspondent-2")
        .build();

    var pagedResponse = PagedResponse.<CorrespondentResponse>builder()
        .count(2)
        .next(null)
        .previous(null)
        .results(List.of(correspondent1, correspondent2))
        .build();

    when(webClient.getCorrespondentsByPage(1)).thenReturn(Mono.just(pagedResponse));
    when(correspondentMapper.toDto(correspondent1))
        .thenReturn(consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Correspondent.builder()
            .id(1)
            .name("Correspondent 1")
            .slug("correspondent-1")
            .build());
    when(correspondentMapper.toDto(correspondent2))
        .thenReturn(consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Correspondent.builder()
            .id(2)
            .name("Correspondent 2")
            .slug("correspondent-2")
            .build());

    // When & Then
    StepVerifier.create(correspondentService.getAll())
        .expectNextMatches(correspondents ->
            correspondents.size() == 2 &&
            correspondents.get(0).getName().equals("Correspondent 1") &&
            correspondents.get(1).getName().equals("Correspondent 2"))
        .verifyComplete();
  }

  @Test
  void getById_shouldHandleNotFound() {
    // Given
    var correspondentId = 999;
    var emptyPagedResponse = PagedResponse.<CorrespondentResponse>builder()
        .count(0)
        .next(null)
        .previous(null)
        .results(List.of())
        .build();

    when(webClient.getCorrespondentsByPage(1)).thenReturn(Mono.just(emptyPagedResponse));
    when(webClient.getCorrespondent(correspondentId)).thenReturn(Mono.error(new RuntimeException("Not found")));

    // When & Then
    StepVerifier.create(correspondentService.getById(correspondentId))
        .expectError(RuntimeException.class)
        .verify();
  }

  @Test
  void getAll_shouldHandleEmptyResponse() {
    // Given
    var emptyPagedResponse = PagedResponse.<CorrespondentResponse>builder()
        .count(0)
        .next(null)
        .previous(null)
        .results(List.of())
        .build();

    when(webClient.getCorrespondentsByPage(1)).thenReturn(Mono.just(emptyPagedResponse));

    // When & Then
    StepVerifier.create(correspondentService.getAll())
        .expectNextMatches(correspondents -> correspondents.isEmpty())
        .verifyComplete();
  }

  @Test
  void getAll_shouldHandleMultiplePages() {
    // Given
    var correspondent1 = CorrespondentResponse.builder()
        .id(1)
        .name("Correspondent 1")
        .slug("correspondent-1")
        .build();

    var correspondent2 = CorrespondentResponse.builder()
        .id(2)
        .name("Correspondent 2")
        .slug("correspondent-2")
        .build();

    var page1Response = PagedResponse.<CorrespondentResponse>builder()
        .count(2)
        .next("http://localhost/api/correspondents/?page=2")
        .previous(null)
        .results(List.of(correspondent1))
        .build();

    var page2Response = PagedResponse.<CorrespondentResponse>builder()
        .count(2)
        .next(null)
        .previous("http://localhost/api/correspondents/?page=1")
        .results(List.of(correspondent2))
        .build();

    when(webClient.getCorrespondentsByPage(1)).thenReturn(Mono.just(page1Response));
    when(webClient.getCorrespondentsByPage(2)).thenReturn(Mono.just(page2Response));

    when(correspondentMapper.toDto(correspondent1))
        .thenReturn(consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Correspondent.builder()
            .id(1)
            .name("Correspondent 1")
            .slug("correspondent-1")
            .build());
    when(correspondentMapper.toDto(correspondent2))
        .thenReturn(consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Correspondent.builder()
            .id(2)
            .name("Correspondent 2")
            .slug("correspondent-2")
            .build());

    // When & Then
    StepVerifier.create(correspondentService.getAll())
        .expectNextMatches(correspondents ->
            correspondents.size() == 2 &&
            correspondents.stream().anyMatch(c -> c.getName().equals("Correspondent 1")) &&
            correspondents.stream().anyMatch(c -> c.getName().equals("Correspondent 2")))
        .verifyComplete();
  }
}
