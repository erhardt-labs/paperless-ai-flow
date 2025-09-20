package consulting.erhardt.paperless_ai_flow.integration;

import consulting.erhardt.paperless_ai_flow.config.PipelineConfiguration;
import consulting.erhardt.paperless_ai_flow.paperless.model.PaperlessDocument;
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
     * Final result handler - processes OCR results from ocrResultChannel
     */
    @ServiceActivator(inputChannel = "ocrResultChannel")
    public void handleOcrResult(Message<String> message) {
        var ocrResult = message.getPayload();
        var pipelineName = (String) message.getHeaders().get("pipelineName");
        var originalDocument = (PaperlessDocument) message.getHeaders().get("originalDocument");
        var hasError = message.getHeaders().containsKey("ocrError");
        
        if (hasError) {
            log.error("OCR processing failed for document {} from pipeline '{}': {}", 
                    originalDocument.getId(), pipelineName, ocrResult);
        } else {
            log.info("OCR result received for document {} from pipeline '{}', markdown length: {}", 
                    originalDocument.getId(), pipelineName, ocrResult.length());
            log.debug("OCR result preview: {}", 
                    ocrResult.length() > 200 ? ocrResult.substring(0, 200) + "..." : ocrResult);
        }
        
        // TODO: Here you could send the result to another channel for further processing
        // For now, we just log the successful completion of the OCR pipeline
    }
}
