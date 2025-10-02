package consulting.erhardt.paperless_ai_flow.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.MessageChannel;

@Configuration
@Profile("test")
public class TestChannelConfig {
  @Bean
  public QueueChannel pollingChannel() {
    var channel = new QueueChannel(2);
    channel.setComponentName("pollingChannel");
    return channel;
  }

  @Bean
  public MessageChannel ocrResultChannel() {
    var channel = new DirectChannel();
    channel.setComponentName("ocrResultChannel");
    return channel;
  }

  @Bean
  public MessageChannel metadataExtractChannel() {
    var channel = new DirectChannel();
    channel.setComponentName("metadataExtractChannel");
    return channel;
  }

  @Bean
  public MessageChannel metadataResultChannel() {
    var channel = new DirectChannel();
    channel.setComponentName("metadataResultChannel");
    return channel;
  }

  @Bean
  public MessageChannel finishedDocumentChannel() {
    var channel = new DirectChannel();
    channel.setComponentName("finishedDocumentChannel");
    return channel;
  }
}

