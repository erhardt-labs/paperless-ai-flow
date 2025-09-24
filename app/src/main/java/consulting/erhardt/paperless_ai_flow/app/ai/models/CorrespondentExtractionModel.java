package consulting.erhardt.paperless_ai_flow.app.ai.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import consulting.erhardt.paperless_ai_flow.app.ai.dtos.CorrespondentDto;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services.CorrespondentService;
import consulting.erhardt.paperless_ai_flow.utils.FileUtils;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class CorrespondentExtractionModel extends AbstractAiModel<CorrespondentDto> {

  private final CorrespondentService correspondentService;

  public CorrespondentExtractionModel(
    OpenAiChatModel openAiChatModel,
    ObjectMapper objectMapper,
    CorrespondentService correspondentService
  ) {
    super(openAiChatModel, objectMapper);

    this.correspondentService = correspondentService;
  }

  @Override
  protected String getSystemPrompt() throws IOException {
    return FileUtils.readFileFromResources("prompts/correspondent.md");
  }

  @Override
  protected String getJsonSchema() throws IOException {
    return FileUtils.readFileFromResources("schemas/correspondent.md");
  }

  @Override
  protected String getDefaultModel() {
    return "openai/o4-mini";
  }

  @Override
  protected Class<CorrespondentDto> getResponseClass() {
    return CorrespondentDto.class;
  }

  @Override
  protected String getUserPrompt(String content) {
    var available = correspondentService.getAll().block();
    var prompt = new StringBuilder();

    // add correspondents
    prompt.append("### Available correspondents:\n");
    for (var correspondent : available) {
      prompt
        .append("- ID: ")
        .append(correspondent.getId())
        .append(", Name: ")
        .append("\"")
        .append(correspondent.getName())
        .append("\"")
        .append("\n");
    }
    prompt.append("\n");

    // add content
    addDocumentContent(prompt, content);

    return prompt.toString();
  }
}
