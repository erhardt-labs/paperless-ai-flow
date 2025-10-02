package consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services;

import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.PaperlessNgxApiClient;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Tag;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities.PagedResponse;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities.TagResponse;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.mappers.TagMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.when;

/**
 * Unit tests for TagService using Mockito to mock dependencies.
 */
@ExtendWith(MockitoExtension.class)
class TagServiceTest {

  @Mock
  private PaperlessNgxApiClient webClient;

  @Mock
  private TagMapper tagMapper;

  @Mock
  private CacheManager cacheManager;

  private TagService tagService;

  @BeforeEach
  void setup() {
    tagService = new TagService(webClient, tagMapper, cacheManager);
  }

  @Test
  @DisplayName("Should get all tags")
  void getAll_shouldReturnAllTags() {
    // Arrange
    var tag1Response = TagResponse.builder()
      .id(1)
      .name("Invoice")
      .slug("invoice")
      .color("#FF0000")
      .textColor("#FFFFFF")
      .build();

    var tag2Response = TagResponse.builder()
      .id(2)
      .name("Receipt")
      .slug("receipt")
      .color("#00FF00")
      .textColor("#000000")
      .build();

    var pagedResponse = PagedResponse.<TagResponse>builder()
      .count(2)
      .next(null)
      .previous(null)
      .results(List.of(tag1Response, tag2Response))
      .build();

    when(webClient.getTagsByPage(1)).thenReturn(Mono.just(pagedResponse));
    when(tagMapper.toDto(tag1Response)).thenReturn(Tag.builder()
      .id(1)
      .name("Invoice")
      .slug("invoice")
      .color("#FF0000")
      .textColor("#FFFFFF")
      .build());
    when(tagMapper.toDto(tag2Response)).thenReturn(Tag.builder()
      .id(2)
      .name("Receipt")
      .slug("receipt")
      .color("#00FF00")
      .textColor("#000000")
      .build());

    // Act & Assert
    StepVerifier.create(tagService.getAll())
      .expectNextMatches(tags ->
        tags.size() == 2 &&
        tags.get(0).getName().equals("Invoice") &&
        tags.get(1).getName().equals("Receipt"))
      .verifyComplete();
  }

  @Test
  @DisplayName("Should handle empty results")
  void getAll_emptyResults_returnsEmptyList() {
    // Arrange
    var emptyPagedResponse = PagedResponse.<TagResponse>builder()
      .count(0)
      .next(null)
      .previous(null)
      .results(List.of())
      .build();

    when(webClient.getTagsByPage(1)).thenReturn(Mono.just(emptyPagedResponse));

    // Act & Assert
    StepVerifier.create(tagService.getAll())
      .expectNextMatches(List::isEmpty)
      .verifyComplete();
  }

  @Test
  @DisplayName("Should get tag by ID")
  void getById_existingTag_returnsTag() {
    // Arrange
    var tagResponse = TagResponse.builder()
      .id(1)
      .name("Invoice")
      .slug("invoice")
      .color("#FF0000")
      .textColor("#FFFFFF")
      .build();

    var pagedResponse = PagedResponse.<TagResponse>builder()
      .count(1)
      .next(null)
      .previous(null)
      .results(List.of(tagResponse))
      .build();

    when(webClient.getTagsByPage(1)).thenReturn(Mono.just(pagedResponse));
    when(tagMapper.toDto(tagResponse)).thenReturn(Tag.builder()
      .id(1)
      .name("Invoice")
      .slug("invoice")
      .color("#FF0000")
      .textColor("#FFFFFF")
      .build());

    // Act & Assert
    StepVerifier.create(tagService.getById(1))
      .expectNextMatches(tag ->
        tag.getId().equals(1) &&
        tag.getName().equals("Invoice") &&
        tag.getSlug().equals("invoice"))
      .verifyComplete();
  }

  @Test
  @DisplayName("Should handle 404 for non-existent tag")
  void getById_nonExistentTag_returnsError() {
    // Arrange
    var emptyPagedResponse = PagedResponse.<TagResponse>builder()
      .count(0)
      .next(null)
      .previous(null)
      .results(List.of())
      .build();

    when(webClient.getTagsByPage(1)).thenReturn(Mono.just(emptyPagedResponse));
    when(webClient.getTag(999)).thenReturn(Mono.error(new RuntimeException("Not found")));

    // Act & Assert
    StepVerifier.create(tagService.getById(999))
      .expectError(RuntimeException.class)
      .verify();
  }

  @Test
  @DisplayName("Should get tag by name")
  void getByName_existingTag_returnsTag() {
    // Arrange
    var tagResponse = TagResponse.builder()
      .id(1)
      .name("Invoice")
      .slug("invoice")
      .color("#FF0000")
      .textColor("#FFFFFF")
      .build();

    var pagedResponse = PagedResponse.<TagResponse>builder()
      .count(1)
      .next(null)
      .previous(null)
      .results(List.of(tagResponse))
      .build();

    when(webClient.getTagsByPage(1)).thenReturn(Mono.just(pagedResponse));
    when(tagMapper.toDto(tagResponse)).thenReturn(Tag.builder()
      .id(1)
      .name("Invoice")
      .slug("invoice")
      .color("#FF0000")
      .textColor("#FFFFFF")
      .build());

    // Act & Assert
    StepVerifier.create(tagService.getByName("Invoice"))
      .expectNextMatches(tag -> tag.getName().equals("Invoice"))
      .verifyComplete();
  }

  @Test
  @DisplayName("Should return empty when tag name not found")
  void getByName_nonExistentTag_returnsEmpty() {
    // Arrange
    var tagResponse = TagResponse.builder()
      .id(1)
      .name("Invoice")
      .slug("invoice")
      .color("#FF0000")
      .textColor("#FFFFFF")
      .build();

    var pagedResponse = PagedResponse.<TagResponse>builder()
      .count(1)
      .next(null)
      .previous(null)
      .results(List.of(tagResponse))
      .build();

    when(webClient.getTagsByPage(1)).thenReturn(Mono.just(pagedResponse));
    when(tagMapper.toDto(tagResponse)).thenReturn(Tag.builder()
      .id(1)
      .name("Invoice")
      .slug("invoice")
      .color("#FF0000")
      .textColor("#FFFFFF")
      .build());

    // Act & Assert
    StepVerifier.create(tagService.getByName("NonExistent"))
      .verifyComplete();
  }

  @Test
  @DisplayName("Should get tag IDs by names")
  void getTagIdsByNames_existingTags_returnsIds() {
    // Arrange
    var tag1Response = TagResponse.builder()
      .id(1)
      .name("Invoice")
      .slug("invoice")
      .color("#FF0000")
      .textColor("#FFFFFF")
      .build();

    var tag2Response = TagResponse.builder()
      .id(2)
      .name("Receipt")
      .slug("receipt")
      .color("#00FF00")
      .textColor("#000000")
      .build();

    var pagedResponse = PagedResponse.<TagResponse>builder()
      .count(2)
      .next(null)
      .previous(null)
      .results(List.of(tag1Response, tag2Response))
      .build();

    when(webClient.getTagsByPage(1)).thenReturn(Mono.just(pagedResponse));
    when(tagMapper.toDto(tag1Response)).thenReturn(Tag.builder()
      .id(1)
      .name("Invoice")
      .slug("invoice")
      .color("#FF0000")
      .textColor("#FFFFFF")
      .build());
    when(tagMapper.toDto(tag2Response)).thenReturn(Tag.builder()
      .id(2)
      .name("Receipt")
      .slug("receipt")
      .color("#00FF00")
      .textColor("#000000")
      .build());

    // Act & Assert
    StepVerifier.create(tagService.getTagIdsByNames(List.of("Invoice", "Receipt")))
      .expectNextMatches(ids -> ids.size() == 2 && ids.contains(1) && ids.contains(2))
      .verifyComplete();
  }

  @Test
  @DisplayName("Should handle multiple pages")
  void getAll_multiplePages_returnsAllTags() {
    // Arrange
    var tag1Response = TagResponse.builder()
      .id(1)
      .name("Tag1")
      .slug("tag1")
      .color("#FF0000")
      .textColor("#FFFFFF")
      .build();

    var tag2Response = TagResponse.builder()
      .id(2)
      .name("Tag2")
      .slug("tag2")
      .color("#00FF00")
      .textColor("#000000")
      .build();

    var page1Response = PagedResponse.<TagResponse>builder()
      .count(2)
      .next("http://localhost/api/tags/?page=2")
      .previous(null)
      .results(List.of(tag1Response))
      .build();

    var page2Response = PagedResponse.<TagResponse>builder()
      .count(2)
      .next(null)
      .previous("http://localhost/api/tags/?page=1")
      .results(List.of(tag2Response))
      .build();

    when(webClient.getTagsByPage(1)).thenReturn(Mono.just(page1Response));
    when(webClient.getTagsByPage(2)).thenReturn(Mono.just(page2Response));
    when(tagMapper.toDto(tag1Response)).thenReturn(Tag.builder()
      .id(1)
      .name("Tag1")
      .slug("tag1")
      .color("#FF0000")
      .textColor("#FFFFFF")
      .build());
    when(tagMapper.toDto(tag2Response)).thenReturn(Tag.builder()
      .id(2)
      .name("Tag2")
      .slug("tag2")
      .color("#00FF00")
      .textColor("#000000")
      .build());

    // Act & Assert
    StepVerifier.create(tagService.getAll())
      .expectNextMatches(tags ->
        tags.size() == 2 &&
        tags.stream().anyMatch(t -> t.getName().equals("Tag1")) &&
        tags.stream().anyMatch(t -> t.getName().equals("Tag2")))
      .verifyComplete();
  }
}
