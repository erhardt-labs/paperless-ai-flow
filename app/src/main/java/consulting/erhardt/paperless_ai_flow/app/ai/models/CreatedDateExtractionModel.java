package consulting.erhardt.paperless_ai_flow.app.ai.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import consulting.erhardt.paperless_ai_flow.app.ai.dtos.CreatedDateDto;
import consulting.erhardt.paperless_ai_flow.app.ai.dtos.TitleDto;
import consulting.erhardt.paperless_ai_flow.utils.FileUtils;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class CreatedDateExtractionModel extends AbstractAiModel<CreatedDateDto> {

  public CreatedDateExtractionModel(
    OpenAiChatModel openAiChatModel,
    ObjectMapper objectMapper
  ) {
    super(openAiChatModel, objectMapper);
  }

  @Override
  protected String getDefaultSystemPrompt() throws IOException {
    return FileUtils.readFileFromResources("prompts/created-date.md");
  }

  @Override
  protected String getJsonSchema() throws IOException {
    return FileUtils.readFileFromResources("schemas/created-date.json");
  }

  @Override
  protected String getDefaultModel() {
    return "openai/o4-mini";
  }

  @Override
  protected Class<CreatedDateDto> getResponseClass() {
    return CreatedDateDto.class;
  }

  @Override
  protected String getUserPrompt(String content) {
    var prompt = new StringBuilder();

    // add content
    addDocumentContent(prompt, content);

    return prompt.toString();
  }
}

