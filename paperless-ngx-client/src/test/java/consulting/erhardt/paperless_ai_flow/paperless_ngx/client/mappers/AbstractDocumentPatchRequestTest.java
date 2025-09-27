package consulting.erhardt.paperless_ai_flow.paperless_ngx.client.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.entities.DocumentPatchRequest;
import org.junit.jupiter.api.BeforeAll;

import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Abstract base class for DocumentPatchRequest JSON serialization and validation tests.
 * Provides common utilities for schema validation, JSON serialization, and field presence assertions.
 */
public abstract class AbstractDocumentPatchRequestTest {

  protected static final ObjectMapper MAPPER = new ObjectMapper();
  protected static JsonSchema SCHEMA;

  @BeforeAll
  static void setup() throws Exception {
    // Register JavaTimeModule so LocalDate honors @JsonFormat
    MAPPER.registerModule(new JavaTimeModule());

    // Load JSON Schema from test resources
    try (var is = AbstractDocumentPatchRequestTest.class.getResourceAsStream("/schemas/PatchedDocumentRequest.json")) {
      assertNotNull(is, "Schema resource not found at /schemas/PatchedDocumentRequest.json");
      var schemaNode = MAPPER.readTree(is);
      var factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
      SCHEMA = factory.getSchema(schemaNode);
    }
  }

  /** Serialize a request and return the parsed ObjectNode. */
  protected static ObjectNode toJson(DocumentPatchRequest req) throws JsonProcessingException {
    var json = MAPPER.writeValueAsString(req);
    var node = MAPPER.readTree(json);
    assertTrue(node.isObject(), "Serialized JSON must be an object");
    return (ObjectNode) node;
  }

  /** Validate against schema. */
  protected static void assertSchemaValid(ObjectNode node) {
    Set<ValidationMessage> errors = SCHEMA.validate(node);
    assertTrue(errors.isEmpty(), () -> "Schema violations:\n" + String.join("\n",
      errors.stream().map(ValidationMessage::toString).toList()));
  }

  /** Assert JSON has exactly expected keys (no more, no less). */
  protected static void assertExactFields(ObjectNode node, Set<String> expectedKeys) {
    var actual = new LinkedHashSet<String>();
    node.fieldNames().forEachRemaining(actual::add);
    assertEquals(expectedKeys, actual, "JSON must contain exactly the expected fields");
  }

  /** Always required checks for remove_inbox_tags. */
  protected static void assertRemoveInboxTags(ObjectNode node, boolean expectedValue) {
    assertTrue(node.has("remove_inbox_tags"), "'remove_inbox_tags' must be present");
    assertTrue(node.get("remove_inbox_tags").isBoolean(), "'remove_inbox_tags' must be boolean");
    assertEquals(expectedValue, node.get("remove_inbox_tags").asBoolean(),
      "remove_inbox_tags must match expected value");
  }
}
