package consulting.erhardt.paperless_ai_flow.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdLockRegistryServiceTest {

    private IdLockRegistryService idLockRegistryService;

    @BeforeEach
    void setUp() {
        idLockRegistryService = new IdLockRegistryService();
    }

    @Test
    void shouldSuccessfullyLockFirstTime() {
        // Given
        var documentId = 123;

        // When
        var lockAcquired = idLockRegistryService.tryLock(documentId);

        // Then
        assertThat(lockAcquired).isTrue();
    }

    @Test
    void shouldFailToLockAlreadyLockedDocument() {
        // Given
        var documentId = 456;
        idLockRegistryService.tryLock(documentId); // First lock

        // When
        var secondLockAttempt = idLockRegistryService.tryLock(documentId);

        // Then
        assertThat(secondLockAttempt).isFalse();
    }

    @Test
    void shouldUnlockAndAllowRelocking() {
        // Given
        var documentId = 789;
        idLockRegistryService.tryLock(documentId);

        // When
        idLockRegistryService.unlock(documentId);
        var relockAttempt = idLockRegistryService.tryLock(documentId);

        // Then
        assertThat(relockAttempt).isTrue();
    }

    @Test
    void shouldHandleDifferentDocumentIds() {
        // Given
        var docId1 = 100;
        var docId2 = 200;

        // When
        var lock1 = idLockRegistryService.tryLock(docId1);
        var lock2 = idLockRegistryService.tryLock(docId2);

        // Then
        assertThat(lock1).isTrue();
        assertThat(lock2).isTrue();
    }

    @Test
    void shouldUnlockNonLockedDocument() {
        // Given
        var documentId = 999;

        // When & Then
        // Should not throw exception when unlocking non-locked document
        idLockRegistryService.unlock(documentId);
    }

    @Test
    void shouldHandleNullDocumentId() {
        // When & Then
        // Should handle null gracefully (depends on implementation)
        try {
            idLockRegistryService.tryLock(null);
            idLockRegistryService.unlock(null);
        } catch (Exception e) {
            // Expected if implementation doesn't handle nulls
            assertThat(e).isInstanceOf(NullPointerException.class);
        }
    }

    @Test
    void shouldCreateService() {
        // When
        var service = new IdLockRegistryService();

        // Then
        assertThat(service).isNotNull();
    }

    @Test
    void shouldHandleConcurrentAccess() {
        // Given
        var documentId = 555;

        // When - Simulate concurrent access
        var lock1 = idLockRegistryService.tryLock(documentId);
        var lock2 = idLockRegistryService.tryLock(documentId);
        var lock3 = idLockRegistryService.tryLock(documentId);

        // Then
        assertThat(lock1).isTrue();
        assertThat(lock2).isFalse();
        assertThat(lock3).isFalse();

        // When unlocked
        idLockRegistryService.unlock(documentId);
        var lock4 = idLockRegistryService.tryLock(documentId);

        // Then
        assertThat(lock4).isTrue();
    }

    @Test
    void shouldHandleMultipleLockUnlockCycles() {
        // Given
        var documentId = 777;

        // When & Then - Multiple cycles
        for (int i = 0; i < 5; i++) {
            assertThat(idLockRegistryService.tryLock(documentId)).isTrue();
            assertThat(idLockRegistryService.tryLock(documentId)).isFalse();
            idLockRegistryService.unlock(documentId);
        }
    }
}
