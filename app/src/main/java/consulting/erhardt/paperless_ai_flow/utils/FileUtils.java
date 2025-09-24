package consulting.erhardt.paperless_ai_flow.utils;

import lombok.experimental.UtilityClass;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@UtilityClass
public class FileUtils {
  public static String readFileFromResources(String path) throws IOException {
    var resource = new ClassPathResource(path);
    try (var inputStream = resource.getInputStream()) {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
