package consulting.erhardt.paperless_ai_flow.app.ai.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Tag;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services.TagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.openai.OpenAiChatModel;
import reactor.core.publisher.Mono;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagExtractionModelTest {

    @Mock
    private OpenAiChatModel openAiChatModel;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private TagService tagService;

    private TagExtractionModel tagExtractionModel;

    @BeforeEach
    void setUp() {
        tagExtractionModel = new TagExtractionModel(
                openAiChatModel,
                objectMapper,
                tagService
        );
    }

    @Test
    void getUserPrompt_shouldReturnPromptWithTagsAndDocumentContent() {
        // Given
        var documentContent = "Important financial document from 2024";
        var tags = Arrays.asList(
                Tag.builder()
                        .id(1)
                        .name("Invoice")
                        .slug("invoice")
                        .color("#FF0000")
                        .textColor("#FFFFFF")
                        .build(),
                Tag.builder()
                        .id(2)
                        .name("Financial")
                        .slug("financial")
                        .color("#00FF00")
                        .textColor("#000000")
                        .build()
        );

        when(tagService.getAll()).thenReturn(Mono.just(tags));

        // When
        var result = tagExtractionModel.getUserPrompt(documentContent);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("### Available tags:");
        assertThat(result).contains("- ID: 1, Name: \"Invoice\"");
        assertThat(result).contains("- ID: 2, Name: \"Financial\"");
        assertThat(result).contains("### Document content:");
        assertThat(result).contains("```");
        assertThat(result).contains(documentContent);
        assertThat(result).endsWith("```\n");
    }

    @Test
    void getUserPrompt_shouldHandleEmptyTagsList() {
        // Given
        var documentContent = "Sample document content";
        when(tagService.getAll()).thenReturn(Mono.just(Arrays.asList()));

        // When
        var result = tagExtractionModel.getUserPrompt(documentContent);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("### Available tags:");
        assertThat(result).contains("### Document content:");
        assertThat(result).contains(documentContent);
    }

    @Test
    void getUserPrompt_shouldHandleSingleTag() {
        // Given
        var documentContent = "Receipt document";
        var tag = Tag.builder()
                .id(99)
                .name("Receipt")
                .slug("receipt")
                .color("#0000FF")
                .textColor("#FFFFFF")
                .build();

        when(tagService.getAll()).thenReturn(Mono.just(Arrays.asList(tag)));

        // When
        var result = tagExtractionModel.getUserPrompt(documentContent);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("### Available tags:");
        assertThat(result).contains("- ID: 99, Name: \"Receipt\"");
        assertThat(result).contains("### Document content:");
        assertThat(result).contains(documentContent);
    }

    @Test
    void getUserPrompt_shouldHandleTagsWithSpecialCharactersInName() {
        // Given
        var documentContent = "Test document";
        var tag = Tag.builder()
                .id(1)
                .name("Tax & Legal (2024)")
                .slug("tax-legal-2024")
                .color("#FFFF00")
                .textColor("#000000")
                .build();

        when(tagService.getAll()).thenReturn(Mono.just(Arrays.asList(tag)));

        // When
        var result = tagExtractionModel.getUserPrompt(documentContent);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("- ID: 1, Name: \"Tax & Legal (2024)\"");
    }
}
