package consulting.erhardt.paperless_ai_flow.app.ai.dtos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TitleDtoTest {

    private ObjectMapper objectMapper;
    private JsonSchema schema;

    @BeforeEach
    void setUp() throws IOException {
        objectMapper = new ObjectMapper();

        // Load JSON schema
        try (InputStream schemaStream = new ClassPathResource("schemas/title.json").getInputStream()) {
            var factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
            schema = factory.getSchema(schemaStream);
        }
    }

    @Test
    void titleDto_shouldBeValidAgainstJsonSchema() throws Exception {
        // Given
        var titleDto = new TitleDto("Sample Document Title");

        // When
        var json = objectMapper.writeValueAsString(titleDto);
        var jsonNode = objectMapper.readTree(json);
        Set<ValidationMessage> validationMessages = schema.validate(jsonNode);

        // Then
        assertThat(validationMessages).isEmpty();
        assertThat(json).contains("\"title\":\"Sample Document Title\"");
    }

    @Test
    void titleDto_shouldHandleEmptyTitle() throws Exception {
        // Given
        var titleDto = new TitleDto("");

        // When
        var json = objectMapper.writeValueAsString(titleDto);
        var jsonNode = objectMapper.readTree(json);
        Set<ValidationMessage> validationMessages = schema.validate(jsonNode);

        // Then
        assertThat(validationMessages).isEmpty();
        assertThat(json).contains("\"title\":\"\"");
    }

    @Test
    void titleDto_shouldHandleSpecialCharacters() throws Exception {
        // Given
        var titleDto = new TitleDto("Invoice & Receipt (2024) - €1,234.56");

        // When
        var json = objectMapper.writeValueAsString(titleDto);
        var jsonNode = objectMapper.readTree(json);
        Set<ValidationMessage> validationMessages = schema.validate(jsonNode);

        // Then
        assertThat(validationMessages).isEmpty();
        assertThat(json).contains("Invoice & Receipt (2024) - €1,234.56");
    }

    @Test
    void titleDto_shouldDeserializeFromValidJson() throws Exception {
        // Given
        var validJson = "{\"title\":\"Test Document Title\"}";

        // When
        var titleDto = objectMapper.readValue(validJson, TitleDto.class);

        // Then
        assertThat(titleDto).isNotNull();
        assertThat(titleDto.getTitle()).isEqualTo("Test Document Title");
    }

    @Test
    void titleDto_shouldValidateRequiredField() throws Exception {
        // Given - JSON missing required "title" field
        var invalidJson = "{}";
        var jsonNode = objectMapper.readTree(invalidJson);

        // When
        Set<ValidationMessage> validationMessages = schema.validate(jsonNode);

        // Then
        assertThat(validationMessages).isNotEmpty();
        assertThat(validationMessages.toString()).contains("title");
        // The message can be in English "required" or German "Pflichtfeld"
        assertThat(validationMessages.toString().toLowerCase()).containsAnyOf("required", "pflichtfeld");
    }

    @Test
    void titleDto_shouldRejectAdditionalProperties() throws Exception {
        // Given - JSON with additional property
        var invalidJson = "{\"title\":\"Test\",\"extraField\":\"value\"}";
        var jsonNode = objectMapper.readTree(invalidJson);

        // When
        Set<ValidationMessage> validationMessages = schema.validate(jsonNode);

        // Then
        assertThat(validationMessages).isNotEmpty();
        assertThat(validationMessages.toString()).contains("additionalProperties");
    }
}
