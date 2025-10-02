package consulting.erhardt.paperless_ai_flow.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FileUtils verifying classpath resource reading functionality.
 */
class FileUtilsTest {

  @Test
  @DisplayName("Should read file from classpath resources")
  void readFileFromResources_existingFile_returnsContent() throws IOException {
    // Act
    var content = FileUtils.readFileFromResources("test-file.txt");

    // Assert
    assertNotNull(content);
    assertFalse(content.isEmpty());
    assertTrue(content.contains("This is a test file content"));
    assertTrue(content.contains("Line 2 of the test file"));
    assertTrue(content.contains("Line 3 with special characters: äöü@#$%"));
  }

  @Test
  @DisplayName("Should read prompt files from resources")
  void readFileFromResources_promptFile_returnsContent() throws IOException {
    // Act
    var content = FileUtils.readFileFromResources("prompts/title.txt");

    // Assert
    assertNotNull(content);
    assertFalse(content.isEmpty());
  }

  @Test
  @DisplayName("Should read schema files from resources")
  void readFileFromResources_schemaFile_returnsContent() throws IOException {
    // Act
    var content = FileUtils.readFileFromResources("schemas/title.json");

    // Assert
    assertNotNull(content);
    assertFalse(content.isEmpty());
    assertTrue(content.contains("title"), "Schema should contain 'title' field");
  }

  @Test
  @DisplayName("Should throw IOException for non-existent file")
  void readFileFromResources_nonExistentFile_throwsIOException() {
    // Act & Assert
    assertThrows(FileNotFoundException.class, () ->
      FileUtils.readFileFromResources("non-existent-file.txt")
    );
  }

  @Test
  @DisplayName("Should throw IOException for null path")
  void readFileFromResources_nullPath_throwsException() {
    // Act & Assert
    assertThrows(Exception.class, () ->
      FileUtils.readFileFromResources(null)
    );
  }

  @Test
  @DisplayName("Should handle UTF-8 encoding correctly")
  void readFileFromResources_utf8Content_preservesEncoding() throws IOException {
    // Act
    var content = FileUtils.readFileFromResources("test-file.txt");

    // Assert
    assertTrue(content.contains("äöü"), "Should preserve UTF-8 special characters");
  }
}
