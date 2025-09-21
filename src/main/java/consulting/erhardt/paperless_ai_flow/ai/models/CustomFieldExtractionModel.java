package consulting.erhardt.paperless_ai_flow.ai.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import consulting.erhardt.paperless_ai_flow.ai.dtos.CustomFieldsDto;
import consulting.erhardt.paperless_ai_flow.paperless.model.PaperlessCustomField;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CustomFieldExtractionModel extends AbstractAiModel {
    private static final String SYSTEM_PROMPT = """
        I will provide you with the content of a document and a list of available custom fields.
        Your task is to extract relevant information from the document content to fill the custom fields.
        For each custom field, determine if there is relevant information in the document that matches the field's purpose.
        Return a map of field IDs to their extracted values. Only include fields where you found relevant information.
        """;

    private static final String JSON_SCHEMA = """
        {
          "type": "object",
          "properties": {
            "customFields": {
              "type": "object",
              "patternProperties": {
                "^[0-9]+$": {
                  "type": "string"
                }
              },
              "description": "Map of custom field IDs (as strings) to their extracted values."
            }
          },
          "required": ["customFields"],
          "additionalProperties": false
        }
        """;

    private static final OpenAiChatOptions CHAT_OPTIONS = OpenAiChatOptions.builder()
            .model("openai/o4-mini")
            .responseFormat(new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, JSON_SCHEMA))
            .build();

    public CustomFieldExtractionModel(
            OpenAiChatModel openAiChatModel,
            ObjectMapper objectMapper
    ) {
        super(openAiChatModel, objectMapper);
    }

    @SneakyThrows
    public @NonNull Map<Long, String> process(@NonNull String content, @NonNull List<PaperlessCustomField> availableCustomFields) {
        var userPrompt = buildUserPromptWithCustomFields(content, availableCustomFields);
        var result = processModel(userPrompt, CustomFieldsDto.class, SYSTEM_PROMPT, CHAT_OPTIONS).getCustomFields();
        
        // Convert String keys back to Long keys
        return result.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    entry -> Long.parseLong(entry.getKey().toString()),
                    Map.Entry::getValue
                ));
    }

    private String buildUserPromptWithCustomFields(String content, List<PaperlessCustomField> availableCustomFields) {
        var customFieldsSection = new StringBuilder();
        customFieldsSection.append("### Available Custom Fields:\n");
        for (var customField : availableCustomFields) {
            customFieldsSection.append("- ID: ").append(customField.getId())
                    .append(", Name: \"").append(customField.getName()).append("\"")
                    .append(", Type: ").append(customField.getDataType()).append("\n");
        }
        customFieldsSection.append("\n");

        return USER_PROMPT
                .replace("{{CONTENT}}", content)
                .replace("### Input:", "### Input:\n\n" + customFieldsSection.toString() + "### Input:");
    }
}
