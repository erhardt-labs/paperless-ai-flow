package consulting.erhardt.paperless_ai_flow.service;

import consulting.erhardt.paperless_ai_flow.ai.models.CorrespondentExtractionModel;
import consulting.erhardt.paperless_ai_flow.ai.models.CustomFieldExtractionModel;
import consulting.erhardt.paperless_ai_flow.ai.models.TagExtractionModel;
import consulting.erhardt.paperless_ai_flow.ai.models.TitleExtractionModel;
import consulting.erhardt.paperless_ai_flow.paperless.client.PaperlessApiClient;
import consulting.erhardt.paperless_ai_flow.paperless.model.PaperlessCorrespondent;
import consulting.erhardt.paperless_ai_flow.paperless.model.PaperlessCustomField;
import consulting.erhardt.paperless_ai_flow.paperless.model.PaperlessTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentMetadataExtractionServiceTest {
    
    @Mock
    private TitleExtractionModel titleModel;
    
    @Mock
    private TagExtractionModel tagModel;
    
    @Mock
    private CorrespondentExtractionModel correspondentModel;
    
    @Mock
    private CustomFieldExtractionModel customFieldModel;
    
    @Mock
    private PaperlessApiClient paperlessApiClient;
    
    @InjectMocks
    private DocumentMetadataExtractionService service;
    
    @Test
    void extractMetadata_shouldExtractAllMetadataInParallel() {
        // Given
        var content = "Sample document content for testing";
        
        var mockTags = List.of(
                PaperlessTag.builder().id(1L).name("Invoice").build(),
                PaperlessTag.builder().id(2L).name("Important").build()
        );
        
        var mockCorrespondents = List.of(
                PaperlessCorrespondent.builder().id(1L).name("Company ABC").build(),
                PaperlessCorrespondent.builder().id(2L).name("Supplier XYZ").build()
        );
        
        var mockCustomFields = List.of(
                PaperlessCustomField.builder().id(1L).name("Amount").dataType("float").build(),
                PaperlessCustomField.builder().id(2L).name("Date").dataType("date").build()
        );
        
        // Mock Paperless API calls
        when(paperlessApiClient.getTags()).thenReturn(Flux.fromIterable(mockTags));
        when(paperlessApiClient.getCorrespondents()).thenReturn(Flux.fromIterable(mockCorrespondents));
        when(paperlessApiClient.getCustomFields()).thenReturn(Flux.fromIterable(mockCustomFields));
        
        // Mock AI model calls
        when(titleModel.process(content)).thenReturn("Extracted Title");
        when(tagModel.process(eq(content), any())).thenReturn(List.of(1L, 2L));
        when(correspondentModel.process(eq(content), any())).thenReturn(1L);
        when(customFieldModel.process(eq(content), any())).thenReturn(Map.of(1L, "123.45", 2L, "2023-12-01"));
        
        // When & Then
        StepVerifier.create(service.extractMetadata(content))
                .expectNextMatches(result -> {
                    return "Extracted Title".equals(result.getTitle()) &&
                           result.getTagIds().equals(List.of(1L, 2L)) &&
                           Long.valueOf(1L).equals(result.getCorrespondentId()) &&
                           result.getCustomFields().equals(Map.of(1L, "123.45", 2L, "2023-12-01"));
                })
                .verifyComplete();
    }
    
    @Test
    void extractMetadata_shouldHandleEmptyTags() {
        // Given
        var content = "Sample document content";
        
        when(paperlessApiClient.getTags()).thenReturn(Flux.empty());
        when(paperlessApiClient.getCorrespondents()).thenReturn(Flux.empty());
        when(paperlessApiClient.getCustomFields()).thenReturn(Flux.empty());
        
        when(titleModel.process(content)).thenReturn("Test Title");
        
        // When & Then
        StepVerifier.create(service.extractMetadata(content))
                .expectNextMatches(result -> {
                    return "Test Title".equals(result.getTitle()) &&
                           result.getTagIds().isEmpty() &&
                           result.getCorrespondentId() == null &&
                           result.getCustomFields().isEmpty();
                })
                .verifyComplete();
    }
    
    @Test
    void extractMetadata_shouldHandleModelFailuresGracefully() {
        // Given
        var content = "Sample document content";
        
        var mockTags = List.of(PaperlessTag.builder().id(1L).name("Test").build());
        
        when(paperlessApiClient.getTags()).thenReturn(Flux.fromIterable(mockTags));
        when(paperlessApiClient.getCorrespondents()).thenReturn(Flux.empty());
        when(paperlessApiClient.getCustomFields()).thenReturn(Flux.empty());
        
        // Mock title extraction failure
        when(titleModel.process(content)).thenThrow(new RuntimeException("AI model failure"));
        when(tagModel.process(eq(content), any())).thenReturn(List.of(1L));
        
        // When & Then
        StepVerifier.create(service.extractMetadata(content))
                .expectNextMatches(result -> {
                    return "Document Title".equals(result.getTitle()) && // Fallback title
                           result.getTagIds().equals(List.of(1L)) &&
                           result.getCorrespondentId() == null &&
                           result.getCustomFields().isEmpty();
                })
                .verifyComplete();
    }
    
    @Test
    void extractMetadata_shouldHandleApiFailuresGracefully() {
        // Given
        var content = "Sample document content";
        
        when(paperlessApiClient.getTags()).thenReturn(Flux.error(new RuntimeException("API failure")));
        when(paperlessApiClient.getCorrespondents()).thenReturn(Flux.empty());
        when(paperlessApiClient.getCustomFields()).thenReturn(Flux.empty());
        
        // When & Then
        StepVerifier.create(service.extractMetadata(content))
                .expectError(RuntimeException.class)
                .verify();
    }
}
