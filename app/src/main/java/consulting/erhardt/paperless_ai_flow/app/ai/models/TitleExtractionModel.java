package consulting.erhardt.paperless_ai_flow.app.ai.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import consulting.erhardt.paperless_ai_flow.app.ai.dtos.TitleDto;
import consulting.erhardt.paperless_ai_flow.utils.FileUtils;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class TitleExtractionModel extends AbstractAiModel<TitleDto> {

  public TitleExtractionModel(
    OpenAiChatModel openAiChatModel,
    ObjectMapper objectMapper
  ) {
    super(openAiChatModel, objectMapper);
  }

  @Override
  protected String getSystemPrompt() throws IOException {
    return FileUtils.readFileFromResources("prompts/title.md");
  }

  @Override
  protected String getJsonSchema() throws IOException {
    return FileUtils.readFileFromResources("schemas/title.json");
  }

  @Override
  protected String getDefaultModel() {
    return "openai/o4-mini";
  }

  @Override
  protected Class<TitleDto> getResponseClass() {
    return TitleDto.class;
  }

  @Override
  protected String getUserPrompt(String content) {
    var prompt = new StringBuilder();

    // add content
    addDocumentContent(prompt, content);

    return prompt.toString();
  }
}

