package consulting.erhardt.paperless_ai_flow.ai.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractAiModel<T> {
  protected final OpenAiChatModel openAiChatModel;
  protected final ObjectMapper objectMapper;

  public T process(@NonNull String content, String systemPrompt) throws IOException {
    // prepare prompts
    var systemMessage = new SystemMessage((systemPrompt != null) ? systemPrompt : getDefaultSystemPrompt());
    var userPrompt = getUserPrompt(content);
    var userMessage = new UserMessage(userPrompt);

    // process prompt
    var prompt = new Prompt(List.of(systemMessage, userMessage), getChatOptions());
    var response = openAiChatModel.call(prompt);

    return objectMapper.readValue(response.getResult().getOutput().getText(), getResponseClass());
  }

  protected abstract String getDefaultSystemPrompt() throws IOException;

  protected abstract String getJsonSchema() throws IOException;

  protected abstract String getDefaultModel();

  protected abstract String getUserPrompt(String content);

  protected abstract Class<T> getResponseClass();

  protected void addDocumentContent(@NonNull StringBuilder prompt, @NonNull String content) {
    // add content
    prompt.append("### Document content:\n");
    prompt.append("```\n");
    prompt.append(content);
    prompt.append("```\n");
  }

  private OpenAiChatOptions getChatOptions() throws IOException {
    return OpenAiChatOptions.builder()
      .model(getDefaultModel())
      .responseFormat(new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, getJsonSchema()))
      .build();
  }
}
