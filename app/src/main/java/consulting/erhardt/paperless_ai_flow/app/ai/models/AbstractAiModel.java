package consulting.erhardt.paperless_ai_flow.app.ai.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractAiModel {
  protected static final String USER_PROMPT = """
        ### Input:

        **Content:**
        ```
        {{CONTENT}}
        ```
    """;

  protected final OpenAiChatModel openAiChatModel;
  protected final ObjectMapper objectMapper;

  protected <T> T processModel(
    @NonNull String content,
    @NonNull Class<T> classificationClass,
    @NonNull String systemPrompt,
    @NonNull OpenAiChatOptions chatOptions
  ) throws JsonProcessingException {
    // prepare prompts
    var systemMessage = new SystemMessage(systemPrompt);
    var userPrompt = buildUserPrompt(content);
    var userMessage = new UserMessage(userPrompt);

    // process prompt
    var prompt = new Prompt(List.of(systemMessage, userMessage), chatOptions);
    var response = openAiChatModel.call(prompt);

    return objectMapper.readValue(response.getResult().getOutput().getText(), classificationClass);
  }

  protected String buildUserPrompt(String content) {
    return USER_PROMPT
      .replace("{{CONTENT}}", content);
  }
}
