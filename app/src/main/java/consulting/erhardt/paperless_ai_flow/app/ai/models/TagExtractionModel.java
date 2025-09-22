package consulting.erhardt.paperless_ai_flow.app.ai.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import consulting.erhardt.paperless_ai_flow.app.ai.dtos.TagsDto;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Tag;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TagExtractionModel extends AbstractAiModel {
  private static final String SYSTEM_PROMPT = """
    I will provide you with the content of a document and a list of available tags.
    Your task is to select the most appropriate tags from the available list that match the content of the document.
    Return the IDs of the tags that are relevant to this document.
    """;

  private static final String JSON_SCHEMA = """
    {
      "type": "object",
      "properties": {
        "tagIds": {
          "type": "array",
          "items": {
            "type": "integer",
            "format": "int64"
          },
          "description": "List of tag IDs that are relevant to the document."
        }
      },
      "required": ["tagIds"],
      "additionalProperties": false
    }
    """;

  private static final OpenAiChatOptions CHAT_OPTIONS = OpenAiChatOptions.builder()
    .model("openai/o4-mini")
    .responseFormat(new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, JSON_SCHEMA))
    .build();

  public TagExtractionModel(
    OpenAiChatModel openAiChatModel,
    ObjectMapper objectMapper
  ) {
    super(openAiChatModel, objectMapper);
  }

  @SneakyThrows
  public @NonNull List<Long> process(@NonNull String content, @NonNull List<Tag> availableTags) {
    var userPrompt = buildUserPromptWithTags(content, availableTags);
    return processModel(userPrompt, TagsDto.class, SYSTEM_PROMPT, CHAT_OPTIONS).getTagIds();
  }

  private String buildUserPromptWithTags(@NonNull String content, @NonNull List<Tag> availableTags) {
    var tagsSection = new StringBuilder();
    tagsSection.append("### Available Tags:\n");
    for (var tag : availableTags) {
      tagsSection.append("- ID: ").append(tag.getId())
        .append(", Name: \"").append(tag.getName()).append("\"\n");
    }
    tagsSection.append("\n");

    return USER_PROMPT
      .replace("{{CONTENT}}", content)
      .replace("### Input:", "### Input:\n\n" + tagsSection.toString() + "### Input:");
  }
}
