package consulting.erhardt.paperless_ai_flow.app.ai.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import consulting.erhardt.paperless_ai_flow.app.ai.dtos.TagsDto;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services.TagService;
import consulting.erhardt.paperless_ai_flow.utils.FileUtils;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class TagExtractionModel extends AbstractAiModel<TagsDto> {
  private final TagService service;

  public TagExtractionModel(
    OpenAiChatModel openAiChatModel,
    ObjectMapper objectMapper,
    TagService service
  ) {
    super(openAiChatModel, objectMapper);

    this.service = service;
  }

  @Override
  protected String getSystemPrompt() throws IOException {
    return FileUtils.readFileFromResources("prompts/tags.md");
  }

  @Override
  protected String getJsonSchema() throws IOException {
    return FileUtils.readFileFromResources("schemas/tags.md");
  }

  @Override
  protected String getDefaultModel() {
    return "openai/o4-mini";
  }

  @Override
  protected Class<TagsDto> getResponseClass() {
    return TagsDto.class;
  }

  @Override
  protected String getUserPrompt(String content) {
    var available = service.getAll().block();
    var prompt = new StringBuilder();

    // add custom fields
    prompt.append("### Available tags:\n");
    for (var tag : available) {
      prompt
        .append("- ID: ")
        .append(tag.getId())
        .append(", Name: \"")
        .append(tag.getName())
        .append("\"")
        .append("\n");
    }
    prompt.append("\n");

    // add content
    addDocumentContent(prompt, content);

    return prompt.toString();
  }
}
