package consulting.erhardt.paperless_ai_flow.ai.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import consulting.erhardt.paperless_ai_flow.ai.dtos.TitleExtraction;
import consulting.erhardt.paperless_ai_flow.utils.FileUtils;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class TitleExtractionModel extends AbstractAiModel<TitleExtraction> {

  public TitleExtractionModel(
    OpenAiChatModel openAiChatModel,
    ObjectMapper objectMapper
  ) {
    super(openAiChatModel, objectMapper);
  }

  @Override
  protected String getDefaultSystemPrompt() throws IOException {
    return FileUtils.readFileFromResources("prompts/title.txt");
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
  protected Class<TitleExtraction> getResponseClass() {
    return TitleExtraction.class;
  }

  @Override
  protected String getUserPrompt(String content) {
    var prompt = new StringBuilder();

    // add content
    addDocumentContent(prompt, content);

    return prompt.toString();
  }
}

