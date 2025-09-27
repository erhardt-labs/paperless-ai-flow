package consulting.erhardt.paperless_ai_flow.app.config;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@Value
@Builder
@ConfigurationProperties(prefix = "paperless")
public class PipelineConfiguration {

  @NonNull
  ApiConfiguration api;

  @NonNull
  @Builder.Default
  List<PipelineDefinition> pipelines = List.of();

  @Value
  @Builder
  public static class ApiConfiguration {
    @NonNull
    String baseUrl;

    @NonNull
    String token;
  }

  @Value
  @Builder
  public static class PipelineDefinition {
    @NonNull
    String name;

    @NonNull
    SelectorConfiguration selector;

    @NonNull
    @Builder.Default
    PollingConfiguration polling = PollingConfiguration.builder()
      .interval(Duration.ofSeconds(60))
      .enabled(true)
      .build();

    @NonNull
    @Builder.Default
    OcrConfiguration ocr = OcrConfiguration.builder().build();

    @Builder.Default
    ExtractionConfiguration extraction = ExtractionConfiguration.builder().build();

    @NonNull
    @Builder.Default
    List<PatchConfiguration> patches = List.of();

    @Builder.Default
    boolean removeInboxTags = false;

    public PipelineDefinition(
      @NonNull String name,
      @NonNull SelectorConfiguration selector,
      PollingConfiguration polling,
      OcrConfiguration ocr,
      ExtractionConfiguration extraction,
      List<PatchConfiguration> patches,
      Boolean removeInboxTags
    ) {
      this.name = name;
      this.selector = selector;
      this.polling = (polling != null) ? polling : PollingConfiguration.builder()
        .interval(Duration.ofSeconds(60))
        .enabled(true)
        .build();
      this.ocr = (ocr != null) ? ocr : OcrConfiguration.builder().build();
      this.extraction = (extraction != null) ? extraction : ExtractionConfiguration.builder().build();
      this.patches = (patches != null) ? patches : List.of();
      this.removeInboxTags = (removeInboxTags != null) ? removeInboxTags : false;
    }
  }

  @Value
  @Builder
  public static class SelectorConfiguration {
    @NonNull
    @Builder.Default
    List<String> requiredTags = List.of();
  }

  @Value
  @Builder
  public static class PollingConfiguration {
    @NonNull
    @Builder.Default
    Duration interval = Duration.ofSeconds(60);

    @Builder.Default
    boolean enabled = true;
  }

  @Value
  @Builder
  public static class OcrConfiguration {
    @NonNull
    @Builder.Default
    String model = "openai/gpt-4o";

    String prompt;
  }

  @Value
  @Builder
  public static class ExtractionConfiguration {
    @Builder.Default
    boolean title = true;
    String titlePrompt;

    @Builder.Default
    boolean createdDate = true;
    String createdDatePrompt;

    @Builder.Default
    boolean correspondent = true;
    String correspondentPrompt;

    @Builder.Default
    boolean tags = true;
    String tagsPrompt;

    @Builder.Default
    boolean customFields = true;
    String customFieldsPrompt;
  }

  @Value
  @Builder
  public static class PatchConfiguration {
    @NonNull
    PatchAction action;

    @NonNull
    PatchType type;

    @NonNull
    String name;

    String value;
  }

  public enum PatchAction {
    ADD,
    DROP,
    SET
  }

  public enum PatchType {
    TAG,
    CORRESPONDENT,
    CUSTOM_FIELD
  }
}
