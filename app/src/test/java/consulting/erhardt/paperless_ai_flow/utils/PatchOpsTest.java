package consulting.erhardt.paperless_ai_flow.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for PatchOps verifying reactive composition utilities.
 */
class PatchOpsTest {

  @Test
  @DisplayName("Should apply function when source has value")
  void applyIfPresent_sourceHasValue_appliesFunction() {
    // Arrange
    var current = Mono.just(10);
    var source = Mono.just(5);

    // Act
    var result = PatchOps.applyIfPresent(current, source, (cur, val) -> cur + val);

    // Assert
    StepVerifier.create(result)
      .expectNext(15)
      .verifyComplete();
  }

  @Test
  @DisplayName("Should return current value when source is empty")
  void applyIfPresent_sourceIsEmpty_returnsCurrentValue() {
    // Arrange
    var current = Mono.just(10);
    var source = Mono.<Integer>empty();

    // Act
    var result = PatchOps.applyIfPresent(current, source, (cur, val) -> cur + val);

    // Assert
    StepVerifier.create(result)
      .expectNext(10)
      .verifyComplete();
  }

  @Test
  @DisplayName("Should handle string concatenation")
  void applyIfPresent_stringConcatenation_worksCorrectly() {
    // Arrange
    var current = Mono.just("Hello");
    var source = Mono.just(" World");

    // Act
    var result = PatchOps.applyIfPresent(current, source, (cur, val) -> cur + val);

    // Assert
    StepVerifier.create(result)
      .expectNext("Hello World")
      .verifyComplete();
  }

  @Test
  @DisplayName("Should handle complex object transformations")
  void applyIfPresent_complexObjectTransformation_worksCorrectly() {
    // Arrange
    record Person(String name, int age) {}
    var current = Mono.just(new Person("John", 30));
    var source = Mono.just(5);

    // Act
    var result = PatchOps.applyIfPresent(current, source,
      (person, yearsToAdd) -> new Person(person.name(), person.age() + yearsToAdd)
    );

    // Assert
    StepVerifier.create(result)
      .assertNext(person -> {
        assertEquals("John", person.name());
        assertEquals(35, person.age());
      })
      .verifyComplete();
  }

  @Test
  @DisplayName("Should propagate errors from current Mono")
  void applyIfPresent_currentMonoErrors_propagatesError() {
    // Arrange
    var current = Mono.<Integer>error(new RuntimeException("Current error"));
    var source = Mono.just(5);

    // Act
    var result = PatchOps.applyIfPresent(current, source, (cur, val) -> cur + val);

    // Assert
    StepVerifier.create(result)
      .expectErrorMessage("Current error")
      .verify();
  }

  @Test
  @DisplayName("Should propagate errors from source Mono")
  void applyIfPresent_sourceMonoErrors_propagatesError() {
    // Arrange
    var current = Mono.just(10);
    var source = Mono.<Integer>error(new RuntimeException("Source error"));

    // Act
    var result = PatchOps.applyIfPresent(current, source, (cur, val) -> cur + val);

    // Assert
    StepVerifier.create(result)
      .expectErrorMessage("Source error")
      .verify();
  }

  @Test
  @DisplayName("Should throw NullPointerException when current is null")
  void applyIfPresent_currentIsNull_throwsNullPointerException() {
    // Arrange
    Mono<Integer> current = null;
    var source = Mono.just(5);

    // Act & Assert
    assertThrows(NullPointerException.class, () ->
      PatchOps.applyIfPresent(current, source, (cur, val) -> cur + val)
    );
  }

  @Test
  @DisplayName("Should throw NullPointerException when source is null")
  void applyIfPresent_sourceIsNull_throwsNullPointerException() {
    // Arrange
    var current = Mono.just(10);
    Mono<Integer> source = null;

    // Act & Assert
    assertThrows(NullPointerException.class, () ->
      PatchOps.applyIfPresent(current, source, (cur, val) -> cur + val)
    );
  }

  @Test
  @DisplayName("Should throw NullPointerException when applier is null")
  void applyIfPresent_applierIsNull_throwsNullPointerException() {
    // Arrange
    var current = Mono.just(10);
    var source = Mono.just(5);

    // Act & Assert
    assertThrows(NullPointerException.class, () ->
      PatchOps.applyIfPresent(current, source, null)
    );
  }
}
