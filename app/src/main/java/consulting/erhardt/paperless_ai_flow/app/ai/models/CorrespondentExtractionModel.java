package consulting.erhardt.paperless_ai_flow.app.ai.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import consulting.erhardt.paperless_ai_flow.app.ai.dtos.CorrespondentDto;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Correspondent;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CorrespondentExtractionModel extends AbstractAiModel {
  private static final String SYSTEM_PROMPT = """
    I will provide you with the content of a document and a list of available correspondents.
    Your task is to select the most appropriate correspondent from the available list that matches the sender or source of the document.
    Return the ID of the correspondent that is most relevant to this document, or null if no correspondent matches.
    """;

  private static final String JSON_SCHEMA = """
    {
      "type": "object",
      "properties": {
        "correspondentId": {
          "type": ["integer", "null"],
          "format": "int64",
          "description": "ID of the correspondent that matches the document sender, or null if no match."
        }
      },
      "required": ["correspondentId"],
      "additionalProperties": false
    }
    """;

  private static final OpenAiChatOptions CHAT_OPTIONS = OpenAiChatOptions.builder()
    .model("openai/o4-mini")
    .responseFormat(new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, JSON_SCHEMA))
    .build();

  public CorrespondentExtractionModel(
    OpenAiChatModel openAiChatModel,
    ObjectMapper objectMapper
  ) {
    super(openAiChatModel, objectMapper);
  }

  @SneakyThrows
  public Long process(@NonNull String content, @NonNull List<Correspondent> availableCorrespondents) {
    var userPrompt = buildUserPromptWithCorrespondents(content, availableCorrespondents);
    return processModel(userPrompt, CorrespondentDto.class, SYSTEM_PROMPT, CHAT_OPTIONS).getCorrespondentId();
  }

  private String buildUserPromptWithCorrespondents(String content, List<Correspondent> availableCorrespondents) {
    var correspondentsSection = new StringBuilder();
    correspondentsSection.append("### Available Correspondents:\n");
    for (var correspondent : availableCorrespondents) {
      correspondentsSection.append("- ID: ").append(correspondent.getId())
        .append(", Name: \"").append(correspondent.getName()).append("\"\n");
    }
    correspondentsSection.append("\n");

    return USER_PROMPT
      .replace("{{CONTENT}}", content)
      .replace("### Input:", "### Input:\n\n" + correspondentsSection.toString() + "### Input:");
  }
}
