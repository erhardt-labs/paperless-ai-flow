package consulting.erhardt.paperless_ai_flow.services;

import consulting.erhardt.paperless_ai_flow.configs.PipelineConfiguration.PipelineDefinition;
import consulting.erhardt.paperless_ai_flow.ai.ocr.OcrClient;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.dtos.Document;
import consulting.erhardt.paperless_ai_flow.paperless_ngx.client.services.DocumentService;
import consulting.erhardt.paperless_ai_flow.utils.FileUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icepdf.core.exceptions.PDFSecurityException;
import org.icepdf.core.pobjects.PDimension;
import org.icepdf.core.pobjects.Page;
import org.icepdf.core.util.GraphicsRenderingHints;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Service for processing PDF documents through OCR pipeline
 * Downloads PDFs, converts pages to images (ICEpdf), and extracts text using OCR
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfOcrService {

  private static final float TARGET_DPI = 300f;
  private static final float RENDER_SCALE = TARGET_DPI / 72f; // ICEpdf uses 72 DPI baseline
  private static final float RENDER_ROTATION = 0f;
  private static final float JPEG_COMPRESSION_QUALITY = 0.9f;

  private final DocumentService documentService;
  private final OcrClient ocrClient;

  public Mono<String> processDocument(Document document, PipelineDefinition pipelineDefinition) {
    var documentId = document.getId();
    var ocrConfig = pipelineDefinition.getOcr();

    log.info("Processing document {} with OCR model: {}", documentId, ocrConfig.getModel());

    return documentService.downloadById(document.getId())
      .flatMapMany(pdfBytes -> convertPdfToImages(pdfBytes, documentId))
      .collectList()
      .flatMap(images -> processImagesWithOcr(Flux.fromIterable(images), pipelineDefinition))
      .doOnSuccess(result -> log.info("Successfully processed document {} - result length: {}",
        documentId, result.length()))
      .doOnError(error -> log.error("Failed to process document {}: {}",
        documentId, error.getMessage(), error));
  }

  private Flux<BufferedImage> convertPdfToImages(byte[] pdfBytes, @NonNull Integer documentId) {
    return Mono.fromCallable(() -> {
        log.info("Converting PDF of {} bytes to images for document {}", pdfBytes.length, documentId);

        var iceDocument = new org.icepdf.core.pobjects.Document();
        try (var bais = new ByteArrayInputStream(pdfBytes)) {
          iceDocument.setInputStream(bais, null);

          var pageCount = iceDocument.getNumberOfPages();
          log.debug("Converting {} pages of document {} to images", pageCount, documentId);

          return IntStream.range(0, pageCount)
            .mapToObj(pageIndex -> {
              try {
                var image = renderPageToImage(iceDocument, pageIndex);
                log.debug("Converted page {} of document {} to image ({}x{})",
                  pageIndex + 1, documentId, image.getWidth(), image.getHeight());
                return image;
              } catch (Exception e) {
                log.error("Failed to render page {} of document {}: {}", pageIndex + 1, documentId, e.getMessage(), e);
                throw new RuntimeException("Failed to render PDF page", e);
              }
            })
            .toList();
        } catch (PDFSecurityException e) {
          log.error("Encrypted or password-protected PDF for document {}: {}", documentId, e.getMessage(), e);
          throw new RuntimeException("Cannot process encrypted or password-protected PDF", e);
        } catch (IOException e) {
          log.error("Failed to load PDF for document {}: {}", documentId, e.getMessage(), e);
          throw new RuntimeException("Failed to load PDF document", e);
        } finally {
          try {
            iceDocument.dispose();
            log.debug("ICEpdf Document disposed for {}", documentId);
          } catch (Throwable t) {
            log.warn("Error disposing ICEpdf Document for {}: {}", documentId, t.getMessage(), t);
          }
        }
      })
      .flatMapMany(Flux::fromIterable)
      .subscribeOn(Schedulers.boundedElastic());
  }

  private BufferedImage renderPageToImage(@NonNull org.icepdf.core.pobjects.Document document, int pageNumber) {
    try {
      var page = document.getPageTree().getPage(pageNumber);
      page.init();

      PDimension size = page.getSize(Page.BOUNDARY_CROPBOX, RENDER_ROTATION, RENDER_SCALE);
      var width = Math.max(1, (int) size.getWidth());
      var height = Math.max(1, (int) size.getHeight());

      var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      var g2 = image.createGraphics();
      try {
        // Improve text/line quality in rasterization
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        page.paint(g2, GraphicsRenderingHints.PRINT, Page.BOUNDARY_CROPBOX, RENDER_ROTATION, RENDER_SCALE);
      } finally {
        g2.dispose();
      }
      return image;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to render PDF page " + pageNumber, e);
    }
  }

  private Mono<String> processImagesWithOcr(Flux<BufferedImage> images, PipelineDefinition pipeline) {
    return images
      .index()
      .concatMap(indexedImage -> {
        var pageNumber = indexedImage.getT1() + 1;
        var image = indexedImage.getT2();

        log.debug("Processing page {} with OCR", pageNumber);

        return Mono.fromCallable(() -> convertImageToMedia(image))
          .subscribeOn(Schedulers.boundedElastic())
          .flatMap(media -> ocrClient.extractText(pipeline, media))
          .map(text -> String.format("# Page %d%n%n%s%n%n", pageNumber, text))
          .doFinally(sig -> image.flush());
      })
      .collectList()
      .map(pages -> String.join("", pages))
      .map(String::trim);
  }

  private Media convertImageToMedia(@NonNull BufferedImage image) throws IOException {
    try (var baos = new ByteArrayOutputStream()) {
      var writers = ImageIO.getImageWritersByFormatName("jpeg");
      if (!writers.hasNext()) {
        throw new IllegalStateException("No JPEG image writers found");
      }

      var writer = writers.next();
      try (var ios = ImageIO.createImageOutputStream(baos)) {
        writer.setOutput(ios);

        var param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
          param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
          param.setCompressionQuality(JPEG_COMPRESSION_QUALITY);
        }

        writer.write(null, new IIOImage(image, null, null), param);
      } finally {
        writer.dispose();
      }

      var imageBytes = baos.toByteArray();
      var imageResource = new ByteArrayResource(imageBytes) {
        @Override
        public String getFilename() {
          return "page.jpg";
        }
      };
      return new Media(MimeTypeUtils.IMAGE_JPEG, imageResource);
    }
  }
}
