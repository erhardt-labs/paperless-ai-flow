package consulting.erhardt.paperless_ai_flow.app.ai.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import consulting.erhardt.paperless_ai_flow.app.ai.dtos.CustomFieldsDto;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services.CustomFieldsService;
import consulting.erhardt.paperless_ai_flow.utils.FileUtils;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;

@Service
public class CustomFieldExtractionModel extends AbstractAiModel<CustomFieldsDto> {
  private final CustomFieldsService service;

  public CustomFieldExtractionModel(
    OpenAiChatModel openAiChatModel,
    ObjectMapper objectMapper,
    CustomFieldsService service
  ) {
    super(openAiChatModel, objectMapper);

    this.service = service;
  }

  @Override
  protected String getDefaultSystemPrompt() throws IOException {
    return FileUtils.readFileFromResources("prompts/custom-fields.md");
  }

  @Override
  protected String getJsonSchema() throws IOException {
    return FileUtils.readFileFromResources("schemas/custom-fields.json");
  }

  @Override
  protected String getDefaultModel() {
    return "openai/o4-mini";
  }

  @Override
  protected Class<CustomFieldsDto> getResponseClass() {
    return CustomFieldsDto.class;
  }

  @Override
  protected String getUserPrompt(String content) {
    return service.getAll()
      .subscribeOn(Schedulers.boundedElastic())
      .map(available -> {
        var prompt = new StringBuilder();

        // add custom fields
        prompt.append("### Available custom fields:\n");
        for (var customField : available) {
          prompt
            .append("- ID: ")
            .append(customField.getId())
            .append(", Name: \"")
            .append(customField.getName())
            .append("\"")
            .append(", Type: ")
            .append(customField.getDataType())
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
