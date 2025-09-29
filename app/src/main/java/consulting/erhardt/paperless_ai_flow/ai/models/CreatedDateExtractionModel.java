package consulting.erhardt.paperless_ai_flow.ai.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import consulting.erhardt.paperless_ai_flow.ai.dtos.CreatedDateExtraction;
import consulting.erhardt.paperless_ai_flow.utils.FileUtils;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class CreatedDateExtractionModel extends AbstractAiModel<CreatedDateExtraction> {

  public CreatedDateExtractionModel(
    OpenAiChatModel openAiChatModel,
    ObjectMapper objectMapper
  ) {
    super(openAiChatModel, objectMapper);
  }

  @Override
  protected String getDefaultSystemPrompt() throws IOException {
    return FileUtils.readFileFromResources("prompts/created-date.txt");
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
  protected Class<CreatedDateExtraction> getResponseClass() {
    return CreatedDateExtraction.class;
  }

  @Override
  protected String getUserPrompt(String content) {
    var prompt = new StringBuilder();

    // add content
    addDocumentContent(prompt, content);

    return prompt.toString();
  }
}

