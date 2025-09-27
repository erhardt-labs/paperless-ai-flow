package consulting.erhardt.paperless_ai_flow.paperless_ngx.client.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Map;

public class MapAsArraySerializer extends JsonSerializer<Map<Integer, String>> {

  @Override
  public void serialize(Map<Integer, String> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    gen.writeStartArray();
    for (var entry : value.entrySet()) {
      gen.writeStartObject();
      gen.writeNumberField("field", entry.getKey());
      gen.writeStringField("value", entry.getValue());
      gen.writeEndObject();
    }
    gen.writeEndArray();
  }
}


