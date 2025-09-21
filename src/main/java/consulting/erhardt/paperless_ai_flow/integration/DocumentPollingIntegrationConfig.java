package consulting.erhardt.paperless_ai_flow.integration;

import consulting.erhardt.paperless_ai_flow.ai.dtos.DocumentMetadataDto;
import consulting.erhardt.paperless_ai_flow.config.PipelineConfiguration;
import consulting.erhardt.paperless_ai_flow.paperless.model.PaperlessDocument;
import consulting.erhardt.paperless_ai_flow.service.DocumentMetadataExtractionService;
import consulting.erhardt.paperless_ai_flow.service.DocumentPollingService;
import consulting.erhardt.paperless_ai_flow.service.PdfOcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Spring Integration configuration for document processing pipeline
 * Documents flow: Polling -> pollingChannel -> OCR -> ocrResultChannel -> Next Steps
 */
@Configuration
@EnableIntegration
@IntegrationComponentScan
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class DocumentPollingIntegrationConfig {
    
    private final PipelineConfiguration pipelineConfig;
    private final DocumentPollingService pollingService;
    private final PdfOcrService pdfOcrService;
    private final DocumentMetadataExtractionService metadataExtractionService;
    
    /**
     * Channel where polling service puts discovered documents
     */
    @Bean
    public MessageChannel pollingChannel() {
        var channel = new QueueChannel(100);
        channel.setComponentName("pollingChannel");
        return channel;
    }
    
    /**
     * Channel where OCR service puts processed results
     */
    @Bean
    public MessageChannel ocrResultChannel() {
        var channel = new DirectChannel();
        channel.setComponentName("ocrResultChannel");
        return channel;
    }
    
    /**
     * Channel where metadata extraction service puts processed results
     */
    @Bean
    public MessageChannel metadataResultChannel() {
        var channel = new DirectChannel();
        channel.setComponentName("metadataResultChannel");
        return channel;
    }
    
    /**
     * Scheduled polling that puts documents into pollingChannel
     */
    @Scheduled(fixedRate = 30000) // Poll every 30 seconds
    public void pollDocuments() {
        var enabledPipelines = pipelineConfig.getPipelines().stream()
                .filter(pipeline -> pipeline.getPolling().isEnabled())
                .toList();
        
        for (var pipeline : enabledPipelines) {
            try {
                var documents = pollingService.pollDocuments(pipeline);
                if (!documents.isEmpty()) {
                    log.info("Found {} documents for pipeline '{}', sending to pollingChannel", 
                            documents.size(), pipeline.getName());
                    
                    // Send each document to the pollingChannel with pipeline context
                    for (var document : documents) {
                        var message = MessageBuilder
                                .withPayload(document)
                                .setHeader("pipeline", pipeline)
                                .setHeader("pipelineName", pipeline.getName())
                                .build();
                        
                        pollingChannel().send(message);
                        log.debug("Sent document {} (title: '{}') to pollingChannel for pipeline '{}'", 
                                document.getId(), document.getTitle(), pipeline.getName());
                    }
                }
            } catch (Exception e) {
                log.error("Error polling documents for pipeline '{}': {}", 
                        pipeline.getName(), e.getMessage(), e);
            }
        }
    }
    
    /**
     * OCR Service Activator - processes documents from pollingChannel
     */
    @ServiceActivator(inputChannel = "pollingChannel", outputChannel = "ocrResultChannel")
    public Message<String> processDocumentOcr(Message<PaperlessDocument> message) {
        var document = message.getPayload();
        var pipeline = (PipelineConfiguration.PipelineDefinition) message.getHeaders().get("pipeline");
        var pipelineName = (String) message.getHeaders().get("pipelineName");
        
        log.info("Processing OCR for document {} from pipeline '{}'", document.getId(), pipelineName);
        
        try {
            // Process the document through OCR
            var ocrResult = pdfOcrService.processDocument(document, pipeline).block();
            
            log.info("OCR completed for document {} from pipeline '{}', result length: {}", 
                    document.getId(), pipelineName, ocrResult.length());
            
            // Create result message with original headers plus OCR result
            return MessageBuilder
                    .withPayload(ocrResult)
                    .copyHeaders(message.getHeaders())
                    .setHeader("ocrCompleted", true)
                    .setHeader("originalDocument", document)
                    .build();
            
        } catch (Exception e) {
            log.error("Error processing OCR for document {} from pipeline '{}': {}", 
                    document.getId(), pipelineName, e.getMessage(), e);
            
            // Return error message
            return MessageBuilder
                    .withPayload("OCR_ERROR: " + e.getMessage())
                    .copyHeaders(message.getHeaders())
                    .setHeader("ocrError", true)
                    .setHeader("originalDocument", document)
                    .build();
        }
    }
    
    /**
     * Metadata Extraction Service Activator - processes OCR results from ocrResultChannel
     */
    @ServiceActivator(inputChannel = "ocrResultChannel", outputChannel = "metadataResultChannel")
    public Message<DocumentMetadataDto> processMetadataExtraction(Message<String> message) {
        var ocrResult = message.getPayload();
        var pipelineName = (String) message.getHeaders().get("pipelineName");
        var originalDocument = (PaperlessDocument) message.getHeaders().get("originalDocument");
        var hasError = message.getHeaders().containsKey("ocrError");
        
        if (hasError) {
            log.error("OCR processing failed for document {} from pipeline '{}': {}", 
                    originalDocument.getId(), pipelineName, ocrResult);
            
            // Return error message for metadata
            return MessageBuilder
                    .<DocumentMetadataDto>withPayload(null)
                    .copyHeaders(message.getHeaders())
                    .setHeader("metadataError", true)
                    .build();
        }
        
        log.info("Starting metadata extraction for document {} from pipeline '{}'", 
                originalDocument.getId(), pipelineName);
        
        try {
            // Process the OCR result through metadata extraction
            var metadataResult = metadataExtractionService.extractMetadata(ocrResult).block();
            
            log.info("Metadata extraction completed for document {} from pipeline '{}' - Title: '{}', Tags: {}, Correspondent: {}, Custom fields: {}", 
                    originalDocument.getId(), pipelineName, metadataResult.getTitle(), 
                    metadataResult.getTagIds().size(), metadataResult.getCorrespondentId(), 
                    metadataResult.getCustomFields().size());
            
            // Create result message with metadata
            return MessageBuilder
                    .withPayload(metadataResult)
                    .copyHeaders(message.getHeaders())
                    .setHeader("metadataCompleted", true)
                    .build();
            
        } catch (Exception e) {
            log.error("Error processing metadata extraction for document {} from pipeline '{}': {}", 
                    originalDocument.getId(), pipelineName, e.getMessage(), e);
            
            // Return error message
            return MessageBuilder
                    .<DocumentMetadataDto>withPayload(null)
                    .copyHeaders(message.getHeaders())
                    .setHeader("metadataError", true)
                    .build();
        }
    }
    
    /**
     * Final result handler - processes metadata extraction results
     */
    @ServiceActivator(inputChannel = "metadataResultChannel")
    public void handleMetadataResult(Message<DocumentMetadataDto> message) {
        var metadataResult = message.getPayload();
        var pipelineName = (String) message.getHeaders().get("pipelineName");
        var originalDocument = (PaperlessDocument) message.getHeaders().get("originalDocument");
        var hasError = message.getHeaders().containsKey("metadataError");
        
        if (hasError) {
            log.error("Metadata extraction failed for document {} from pipeline '{}'", 
                    originalDocument.getId(), pipelineName);
        } else {
            log.info("Pipeline processing completed for document {} from pipeline '{}' - Extracted metadata:", 
                    originalDocument.getId(), pipelineName);
            log.info("  Title: '{}'", metadataResult.getTitle());
            log.info("  Tag IDs: {}", metadataResult.getTagIds());
            log.info("  Correspondent ID: {}", metadataResult.getCorrespondentId());
            log.info("  Custom fields: {}", metadataResult.getCustomFields());
        }
        
        // TODO: Here you could send the result to Paperless for document update
        // For now, we just log the successful completion of the full pipeline
    }
}
