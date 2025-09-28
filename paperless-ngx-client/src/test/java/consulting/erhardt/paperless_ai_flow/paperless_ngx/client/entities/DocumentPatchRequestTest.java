package consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities;

import com.fasterxml.jackson.databind.node.ArrayNode;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.mappers.AbstractDocumentPatchRequestTest;
import lombok.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Parameterized tests for verifying JSON serialization of DocumentPatchRequest
 * against the provided JSON schema with all field combinations.
 * <p>
 * Guarantees:
 * 1) Serialized JSON conforms to the schema.
 * 2) Only non-null fields are present (nulls omitted).
 * 3) "created" is rendered as YYYY-MM-DD.
 * 4) "remove_inbox_tags" is always present and never null.
 */
class DocumentPatchRequestTest extends AbstractDocumentPatchRequestTest {

  /**
   * All optional data-bearing fields (remove_inbox_tags is handled separately).
   */
  enum F {
    TITLE, CREATED, CONTENT, CORRESPONDENT, TAGS, CUSTOM_FIELDS
  }

  /**
   * Produces all 2^6 = 64 combinations of fields,
   * and for each combination two cases for remove_inbox_tags: default(false) and true.
   */
  static Stream<Arguments> allCombinations() {
    var fields = F.values();
    var max = 1 << fields.length; // 64

    return IntStream.range(0, max)
      .boxed()
      .flatMap(mask -> {
        var set = EnumSet.noneOf(F.class);
        for (int i = 0; i < fields.length; i++) {
          if ((mask & (1 << i)) != 0) set.add(fields[i]);
        }
        // two variants for remove_inbox_tags: default(false) and true
        return Stream.of(
          Arguments.of(Set.copyOf(set), false),
          Arguments.of(Set.copyOf(set), true)
        );
      });
  }


  @ParameterizedTest(name = "[{index}] fields={0}, remove_inbox_tags_true={1}")
  @MethodSource("allCombinations")
  @DisplayName("Serialize DocumentPatchRequest for all field combinations")
  void serialize_allFieldCombinations_validateSchema_andPresence(
    @NonNull Set<F> fields,
    boolean forceRemoveTrue
  ) throws Exception {

    // --- Arrange request with deterministic sample values per field ---
    var builder = DocumentPatchRequest.builder();

    if (fields.contains(F.TITLE)) builder.title("T");
    if (fields.contains(F.CREATED)) builder.created(LocalDate.of(2025, 9, 27));
    if (fields.contains(F.CONTENT)) builder.content("C");
    if (fields.contains(F.CORRESPONDENT)) builder.correspondentId(11);
    if (fields.contains(F.TAGS)) builder.tagIds(List.of(1, 2));
    if (fields.contains(F.CUSTOM_FIELDS)) builder.customFields(Map.of(1, "A"));

    if (forceRemoveTrue) builder.removeInboxTags(true);
    // else: rely on default false via @Builder.Default and @NonNull

    var node = toJson(builder.build());

    // --- Schema validation ---
    assertSchemaValid(node);

    // --- remove_inbox_tags check ---
    assertRemoveInboxTags(node, forceRemoveTrue ? true : false);

    // --- Field presence: exactly the selected ones, with correct JSON names ---
    var expectedKeys = getExpectedKeys(fields);

    assertExactFields(node, expectedKeys);

    // --- Value assertions per included field ---
    if (fields.contains(F.TITLE)) {
      assertEquals("T", node.get("title").asText());
    } else {
      assertFalse(node.has("title"));
    }

    if (fields.contains(F.CREATED)) {
      // strict string comparison to ensure "YYYY-MM-DD"
      assertEquals("2025-09-27", node.get("created").asText());
    } else {
      assertFalse(node.has("created"));
    }

    if (fields.contains(F.CONTENT)) {
      assertEquals("C", node.get("content").asText());
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
      assertEquals("A", cf0.get("value").asText());
    } else {
      assertFalse(node.has("custom_fields"));
    }
  }

  private static LinkedHashSet<String> getExpectedKeys(Set<F> fields) {
    var expectedKeys = new LinkedHashSet<String>();
    if (fields.contains(F.TITLE)) {
      expectedKeys.add("title");
    }
    if (fields.contains(F.CREATED)) {
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

    expectedKeys.add("remove_inbox_tags");
    return expectedKeys;
  }
}
