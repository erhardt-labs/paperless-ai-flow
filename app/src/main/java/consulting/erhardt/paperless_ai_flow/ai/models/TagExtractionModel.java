package consulting.erhardt.paperless_ai_flow.ai.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import consulting.erhardt.paperless_ai_flow.ai.dtos.TagsExtraction;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services.TagService;
import consulting.erhardt.paperless_ai_flow.utils.FileUtils;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;

@Service
public class TagExtractionModel extends AbstractAiModel<TagsExtraction> {
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
  protected String getDefaultSystemPrompt() throws IOException {
    return FileUtils.readFileFromResources("prompts/tags.txt");
  }

  @Override
  protected String getJsonSchema() throws IOException {
    return FileUtils.readFileFromResources("schemas/tags.json");
  }

  @Override
  protected String getDefaultModel() {
    return "openai/o4-mini";
  }

  @Override
  protected Class<TagsExtraction> getResponseClass() {
    return TagsExtraction.class;
  }

  @Override
  protected String getUserPrompt(String content) {
    return service.getAll()
      .subscribeOn(Schedulers.boundedElastic())
      .map(available -> {
        var prompt = new StringBuilder();

        // add tags
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
      })
      .block();
  }
}
