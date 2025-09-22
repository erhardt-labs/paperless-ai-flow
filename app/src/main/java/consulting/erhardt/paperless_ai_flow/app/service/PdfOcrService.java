package consulting.erhardt.paperless_ai_flow.app.service;

import consulting.erhardt.paperless_ai_flow.app.config.PipelineConfiguration.PipelineDefinition;
import consulting.erhardt.paperless_ai_flow.app.ocr.OcrClient;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Document;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services.DocumentService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.stream.IntStream;

/**
 * Service for processing PDF documents through OCR pipeline
 * Downloads PDFs, converts pages to images, and extracts text using OCR
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfOcrService {

  private final DocumentService documentService;
  private final OcrClient ocrClient;

  /**
   * Process a PDF document through OCR pipeline
   *
   * @param document           the Paperless document to process
   * @param pipelineDefinition the pipeline configuration
   * @return combined markdown text from all pages
   */
  public Mono<String> processDocument(Document document, PipelineDefinition pipelineDefinition) {
    var documentId = document.getId();
    var ocrConfig = pipelineDefinition.getOcr();

    log.info("Processing document {} with OCR model: {}", documentId, ocrConfig.getModel());

    return documentService.downloadById(document.getId())
      .flatMapMany(pdfBytes -> convertPdfToImages(pdfBytes, documentId))
      .collectList()
      .flatMap(images -> processImagesWithOcr(Flux.fromIterable(images), ocrConfig.getModel(), ocrConfig.getPrompt()))
      .doOnSuccess(result -> log.info("Successfully processed document {} - result length: {}",
        documentId, result.length()))
      .doOnError(error -> log.error("Failed to process document {}: {}",
        documentId, error.getMessage(), error));
  }

  /**
   * Convert PDF bytes to BufferedImage list using PDFBox
   */
  private Flux<BufferedImage> convertPdfToImages(byte[] pdfBytes, @NonNull Integer documentId) {
    return Mono.fromCallable(() -> {

      log.info("Converting PDF of {} bytes to images for document {}", pdfBytes.length, documentId);

      try (var document = Loader.loadPDF(pdfBytes)) {
        var renderer = new PDFRenderer(document);
        var pageCount = document.getNumberOfPages();

        log.debug("Converting {} pages of document {} to images", pageCount, documentId);

        return IntStream.range(0, pageCount)
          .mapToObj(pageIndex -> {
            try {
              // Render page as image with 300 DPI for good quality
              var image = renderer.renderImageWithDPI(pageIndex, 300, ImageType.RGB);
              log.debug("Converted page {} of document {} to image ({}x{})",
                pageIndex + 1, documentId, image.getWidth(), image.getHeight());
              return image;
            } catch (IOException e) {
              log.error("Failed to render page {} of document {}: {}",
                pageIndex + 1, documentId, e.getMessage(), e);

              throw new RuntimeException("Failed to render PDF page", e);
            }
          })
          .toList();

      } catch (IOException e) {
        log.error("Failed to load PDF for document {}: {}", documentId, e.getMessage(), e);
        throw new RuntimeException("Failed to load PDF document", e);
      }

    }).flatMapMany(Flux::fromIterable);
  }

  /**
   * Process all images with OCR and combine results
   */
  private Mono<String> processImagesWithOcr(Flux<BufferedImage> images, String model, String prompt) {
    return images
      .index()
      .flatMap(indexedImage -> {
        var pageNumber = indexedImage.getT1() + 1;
        var image = indexedImage.getT2();

        log.debug("Processing page {} with OCR", pageNumber);

        return ocrClient.extractText(image, model, prompt)
          .map(text -> String.format("## Page %d%n%n%s%n%n", pageNumber, text))
          .doOnSuccess(result -> log.debug("OCR completed for page {}", pageNumber));
      })
      .collectList()
      .map(pages -> String.join("", pages))
      .map(String::trim);
  }
}
