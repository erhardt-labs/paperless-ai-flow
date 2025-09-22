package consulting.erhardt.paperless_ai_flow.app.paperless.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import lombok.NonNull;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Custom Jackson deserializer for LocalDateTime that handles both date-only
 * and date-time strings from the Paperless API.
 * <p>
 * This deserializer attempts to parse the input as LocalDateTime first,
 * and if that fails, parses as LocalDate and converts to LocalDateTime at midnight.
 */
public class LocalDateTimeFlexibleDeserializer extends JsonDeserializer<LocalDateTime> {

  // Common date-time patterns from Paperless API
  private static final DateTimeFormatter[] DATE_TIME_FORMATTERS = {
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'"),
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX"),
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"),
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"),
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"),
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
  };

  private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  @Override
  public LocalDateTime deserialize(@NonNull JsonParser parser, @NonNull DeserializationContext context) throws IOException {
    var dateString = parser.getValueAsString();

    if (dateString == null || dateString.trim().isEmpty()) {
      return null;
    }

    var trimmedDateString = dateString.trim();

    // Try to parse as LocalDateTime with various patterns
    for (var formatter : DATE_TIME_FORMATTERS) {
      try {
        return LocalDateTime.parse(trimmedDateString, formatter);
      } catch (DateTimeParseException ignored) {
        // Continue to next formatter
      }
    }

    // If all date-time patterns fail, try parsing as date-only and convert to LocalDateTime at midnight
    try {
      var localDate = LocalDate.parse(trimmedDateString, DATE_ONLY_FORMATTER);
      return localDate.atStartOfDay();
    } catch (DateTimeParseException e) {
      throw new IOException("Unable to parse date string: " + trimmedDateString, e);
    }
  }
}
