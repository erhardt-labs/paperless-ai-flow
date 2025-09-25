package consulting.erhardt.paperless_ai_flow.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.MessageChannel;

@Configuration
public class ChannelConfig {
  @Bean
  public MessageChannel pollingChannel() {
    var channel = new QueueChannel(100);
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
}
