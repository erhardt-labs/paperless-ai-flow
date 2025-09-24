package consulting.erhardt.paperless_ai_flow.app.ai.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Correspondent;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services.CorrespondentService;
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
class CorrespondentExtractionModelTest {

    @Mock
    private OpenAiChatModel openAiChatModel;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private CorrespondentService correspondentService;

    private CorrespondentExtractionModel correspondentExtractionModel;

    @BeforeEach
    void setUp() {
        correspondentExtractionModel = new CorrespondentExtractionModel(
                openAiChatModel,
                objectMapper,
                correspondentService
        );
    }

    @Test
    void getUserPrompt_shouldReturnPromptWithCorrespondentsAndDocumentContent() {
        // Given
        var documentContent = "Sample invoice from ACME Corp";
        var correspondents = Arrays.asList(
                Correspondent.builder()
                        .id(1)
                        .name("ACME Corporation")
                        .slug("acme-corp")
                        .build(),
                Correspondent.builder()
                        .id(2)
                        .name("Tech Solutions Ltd")
                        .slug("tech-solutions")
                        .build()
        );

        when(correspondentService.getAll()).thenReturn(Mono.just(correspondents));

        // When
        var result = correspondentExtractionModel.getUserPrompt(documentContent);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("### Available correspondents:");
        assertThat(result).contains("- ID: 1, Name: \"ACME Corporation\"");
        assertThat(result).contains("- ID: 2, Name: \"Tech Solutions Ltd\"");
        assertThat(result).contains("### Document content:");
        assertThat(result).contains("```");
        assertThat(result).contains(documentContent);
        assertThat(result).endsWith("```\n");
    }

    @Test
    void getUserPrompt_shouldHandleEmptyCorrespondentsList() {
        // Given
        var documentContent = "Sample document content";
        when(correspondentService.getAll()).thenReturn(Mono.just(Arrays.asList()));

        // When
        var result = correspondentExtractionModel.getUserPrompt(documentContent);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("### Available correspondents:");
        assertThat(result).contains("### Document content:");
        assertThat(result).contains(documentContent);
    }

    @Test
    void getUserPrompt_shouldHandleSingleCorrespondent() {
        // Given
        var documentContent = "Invoice document";
        var correspondent = Correspondent.builder()
                .id(42)
                .name("Single Corp")
                .slug("single-corp")
                .build();

        when(correspondentService.getAll()).thenReturn(Mono.just(Arrays.asList(correspondent)));

        // When
        var result = correspondentExtractionModel.getUserPrompt(documentContent);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("### Available correspondents:");
        assertThat(result).contains("- ID: 42, Name: \"Single Corp\"");
        assertThat(result).contains("### Document content:");
        assertThat(result).contains(documentContent);
    }

    @Test
    void getUserPrompt_shouldHandleCorrespondentsWithSpecialCharactersInName() {
        // Given
        var documentContent = "Test document";
        var correspondent = Correspondent.builder()
                .id(1)
                .name("Company & Co. (Ltd.)")
                .slug("company-co-ltd")
                .build();

        when(correspondentService.getAll()).thenReturn(Mono.just(Arrays.asList(correspondent)));

        // When
        var result = correspondentExtractionModel.getUserPrompt(documentContent);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("- ID: 1, Name: \"Company & Co. (Ltd.)\"");
    }
}
