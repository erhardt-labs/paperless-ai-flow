package consulting.erhardt.paperless_ai_flow.paperless_ngx.client.mappers;

import com.fasterxml.jackson.databind.node.ArrayNode;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Correspondent;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.CustomField;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Document;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Tag;
import lombok.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Parameterized tests for verifying the DocumentMapper.toPatchRequest() mapping function
 * against the provided JSON schema with all field combinations.
 * <p>
 * Guarantees:
 * 1) Mapped DocumentPatchRequest serialized JSON conforms to the schema.
 * 2) Only non-null fields from Document are present (nulls omitted).
 * 3) "createdDate" is mapped to "created" and rendered as YYYY-MM-DD.
 * 4) "remove_inbox_tags" is always present and defaults to false.
 */
@SpringBootTest
class DocumentMapperTest extends AbstractDocumentPatchRequestTest {

  @Autowired
  private DocumentMapper documentMapper;

  /**
   * All optional data-bearing fields from Document DTO.
   */
  enum F {
    TITLE, CREATED_DATE, CONTENT, CORRESPONDENT, TAGS, CUSTOM_FIELDS
  }

  /**
   * Produces all 2^6 = 64 combinations of Document fields.
   */
  static Stream<Arguments> allDocumentCombinations() {
    var fields = F.values();
    var max = 1 << fields.length; // 64

    return IntStream.range(0, max)
      .boxed()
      .map(mask -> {
        var set = EnumSet.noneOf(F.class);
        for (int i = 0; i < fields.length; i++) {
          if ((mask & (1 << i)) != 0) set.add(fields[i]);
        }
        return Arguments.of(Set.copyOf(set));
      });
  }

  @ParameterizedTest(name = "[{index}] Document fields={0}")
  @MethodSource("allDocumentCombinations")
  @DisplayName("Test DocumentMapper.toPatchRequest() for all Document field combinations")
  void toPatchRequest_allDocumentFieldCombinations_validateSchema_andMapping(
    @NonNull Set<F> fields
  ) throws Exception {

    // --- Arrange Document with deterministic sample values per field ---
    var documentBuilder = Document.builder()
      .id(123); // BaseEntity id is always required

    if (fields.contains(F.TITLE)) {
      documentBuilder.title("Test Document Title");
    }

    if (fields.contains(F.CREATED_DATE)) {
      documentBuilder.createdDate(LocalDate.of(2025, 9, 27));
    }

    if (fields.contains(F.CONTENT)) {
      documentBuilder.content("Test document content for mapping validation");
    }

    if (fields.contains(F.CORRESPONDENT)) {
      var correspondent = Correspondent.builder()
        .id(11)
        .slug("test-correspondent")
        .name("Test Correspondent")
        .build();
      documentBuilder.correspondent(correspondent);
    }

    if (fields.contains(F.TAGS)) {
      var tag1 = Tag.builder()
        .id(1)
        .slug("tag1")
        .name("Tag 1")
        .color("#FF0000")
        .textColor("#FFFFFF")
        .build();

      var tag2 = Tag.builder()
        .id(2)
        .slug("tag2")
        .name("Tag 2")
        .color("#00FF00")
        .textColor("#000000")
        .build();
      documentBuilder.tags(List.of(tag1, tag2));
    }

    if (fields.contains(F.CUSTOM_FIELDS)) {
      var customField1 = CustomField.builder()
        .id(1)
        .name("Test Field")
        .dataType("string")
        .value("Test Value")
        .extraData(Map.of("key", "value"))
        .build();
      documentBuilder.customFields(List.of(customField1));
    }

    var document = documentBuilder.build();

    // --- Act: Map Document to DocumentPatchRequest ---
    var patchRequest = documentMapper.toPatchRequest(document);
    var node = toJson(patchRequest);

    // --- Schema validation ---
    assertSchemaValid(node);

    // --- remove_inbox_tags check (always present with default false) ---
    assertRemoveInboxTags(node, false);

    // --- Field presence: exactly the selected ones, with correct JSON names ---
    var expectedKeys = getExpectedKeysForDocument(fields);
    assertExactFields(node, expectedKeys);

    // --- Value assertions per included field ---
    if (fields.contains(F.TITLE)) {
      assertEquals("Test Document Title", node.get("title").asText());
    } else {
      assertFalse(node.has("title"));
    }

    if (fields.contains(F.CREATED_DATE)) {
      // strict string comparison to ensure "YYYY-MM-DD" format
      assertEquals("2025-09-27", node.get("created").asText());
    } else {
      assertFalse(node.has("created"));
    }

    if (fields.contains(F.CONTENT)) {
      assertEquals("Test document content for mapping validation", node.get("content").asText());
    } else {
      assertFalse(node.has("content"));
    }

    if (fields.contains(F.CORRESPONDENT)) {
      assertEquals(11, node.get("correspondent").asInt());
    } else {
      assertFalse(node.has("correspondent"));
    }

    if (fields.contains(F.TAGS)) {
      assertTrue(node.get("tags").isArray());
      var tags = (ArrayNode) node.get("tags");
      assertEquals(2, tags.size());
      assertEquals(1, tags.get(0).asInt());
      assertEquals(2, tags.get(1).asInt());
    } else {
      assertFalse(node.has("tags"));
    }

    if (fields.contains(F.CUSTOM_FIELDS)) {
      assertTrue(node.get("custom_fields").isArray(), "custom_fields must be an array");
      var arr = (ArrayNode) node.get("custom_fields");
      assertEquals(1, arr.size());
      var cf0 = arr.get(0);
      assertTrue(cf0.has("field") && cf0.has("value"));
      assertEquals(1, cf0.get("field").asInt());
      assertEquals("Test Value", cf0.get("value").asText());
    } else {
      assertFalse(node.has("custom_fields"));
    }
  }

  private static LinkedHashSet<String> getExpectedKeysForDocument(Set<F> fields) {
    var expectedKeys = new LinkedHashSet<String>();

    if (fields.contains(F.TITLE)) {
      expectedKeys.add("title");
    }
    if (fields.contains(F.CREATED_DATE)) {
      expectedKeys.add("created");
    }
    if (fields.contains(F.CONTENT)) {
      expectedKeys.add("content");
    }
    if (fields.contains(F.CORRESPONDENT)) {
      expectedKeys.add("correspondent");
    }
    if (fields.contains(F.TAGS)) {
      expectedKeys.add("tags");
    }
    if (fields.contains(F.CUSTOM_FIELDS)) {
      expectedKeys.add("custom_fields");
    }

    // remove_inbox_tags is always present (default false)
    expectedKeys.add("remove_inbox_tags");
    return expectedKeys;
  }
}
