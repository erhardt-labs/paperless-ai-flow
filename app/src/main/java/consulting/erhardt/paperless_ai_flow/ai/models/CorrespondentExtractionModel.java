package consulting.erhardt.paperless_ai_flow.ai.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import consulting.erhardt.paperless_ai_flow.ai.dtos.CorrespondentExtraction;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services.CorrespondentService;
import consulting.erhardt.paperless_ai_flow.utils.FileUtils;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;

@Service
public class CorrespondentExtractionModel extends AbstractAiModel<CorrespondentExtraction> {

  private final CorrespondentService service;

  public CorrespondentExtractionModel(
    OpenAiChatModel openAiChatModel,
    ObjectMapper objectMapper,
    CorrespondentService correspondentService
  ) {
    super(openAiChatModel, objectMapper);

    this.service = correspondentService;
  }

  @Override
  protected String getDefaultSystemPrompt() throws IOException {
    return FileUtils.readFileFromResources("prompts/correspondent.md");
  }

  @Override
  protected String getJsonSchema() throws IOException {
    return FileUtils.readFileFromResources("schemas/correspondent.json");
  }

  @Override
  protected String getDefaultModel() {
    return "openai/o4-mini";
  }

  @Override
  protected Class<CorrespondentExtraction> getResponseClass() {
    return CorrespondentExtraction.class;
  }

  @Override
  protected String getUserPrompt(String content) {
    return service.getAll()
      .subscribeOn(Schedulers.boundedElastic())
      .map(available -> {
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
      })
      .block();
  }
}
