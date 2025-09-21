package consulting.erhardt.paperless_ai_flow.ai.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import consulting.erhardt.paperless_ai_flow.ai.dtos.TitleDto;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.stereotype.Service;

@Service
public class TitleExtractionModel extends AbstractAiModel {
    private static final String SYSTEM_PROMPT = """
        I will provide you with the content of a document that has been partially read by OCR (so it may contain errors).
        Your task is to find a suitable document title that I can use as the title in the paperless-ngx program.
    """;

    private static final String JSON_SCHEMA = """
        {
          "type": "object",
          "properties": {
            "title": {
              "type": "string",
              "description": "Title used for the document."
            }
          },
          "required": ["title"],
          "additionalProperties": false
        }
        """;

    private static final OpenAiChatOptions CHAT_OPTIONS = OpenAiChatOptions.builder()
            .model("openai/o4-mini")
            .responseFormat(new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, JSON_SCHEMA))
            .build();

    public TitleExtractionModel(
            OpenAiChatModel openAiChatModel,
            ObjectMapper objectMapper
    ) {
        super(openAiChatModel, objectMapper);
    }

    @SneakyThrows
    public @NonNull String process(@NonNull String content) {
        return processModel(content, TitleDto.class, SYSTEM_PROMPT, CHAT_OPTIONS).getTitle();
    }
}

