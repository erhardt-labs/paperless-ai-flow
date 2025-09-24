package consulting.erhardt.paperless_ai_flow.serializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class MapAsArrayDeserializer extends JsonDeserializer<Map<Integer, String>> {

  @Override
  public Map<Integer, String> deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
    var node = (JsonNode) p.getCodec().readTree(p);

    if (!node.isArray()) {
      throw new JsonMappingException(p, "Expected an array for customFields");
    }

    var result = new LinkedHashMap<Integer, String>();
    for (var element : node) {
      var keyNode = element.get("key");
      var valueNode = element.get("value");

      if (keyNode == null || valueNode == null) {
        throw new JsonMappingException(p, "Each element must have 'key' and 'value'");
      }

      var key = keyNode.asInt();
      var value = valueNode.asText();
      result.put(key, value);
    }

    return result;
  }
}
