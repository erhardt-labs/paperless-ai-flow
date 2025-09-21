package consulting.erhardt.paperless_ai_flow.ai.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.util.List;

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
        var systemMessage = new SystemMessage(systemPrompt);
        var userMessage = new UserMessage(buildUserPrompt(content));

        var prompt = new Prompt(List.of(systemMessage, userMessage), chatOptions);
        var response = openAiChatModel.call(prompt);

        return objectMapper.readValue(response.getResult().getOutput().getText(), classificationClass);
    }

    protected String buildUserPrompt(String content) {
        return USER_PROMPT
                .replace("{{CONTENT}}", content);
    }
}
