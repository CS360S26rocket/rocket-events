package com.example.seprojectpart3;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.firebase.firestore.FirebaseFirestore;

import static org.junit.Assert.*;

/**
 * Unit tests for M3 Sprint 4 stories:
 *  - #33  DiscountRepository
 *  - #34  IdempotencyManager
 *  - TKT-A TicketGenerationService
 *  - ORG-A PaymentQRRepository
 */
@RunWith(MockitoJUnitRunner.class)
public class Sprint4M3Tests {

    @Mock FirebaseFirestore mockDb;

    private DiscountRepository discountRepo;
    private IdempotencyManager idempotencyManager;
    private TicketGenerationService ticketService;
    private PaymentQRRepository paymentQRRepo;

    @Before
    public void setUp() {
        discountRepo = new DiscountRepository(mockDb);
        idempotencyManager = new IdempotencyManager(mockDb);
        ticketService = new TicketGenerationService(mockDb, idempotencyManager);
        paymentQRRepo = new PaymentQRRepository(mockDb);
    }

    // ═══ #33 — Discount Code Tests ════════════════════════════

    @Test
    public void validateDiscount_nullCode_returnsFailure() {
        discountRepo.validateAndApply(null, "event1", 100.0, result -> {
            assertFalse(result.valid);
            assertTrue(result.message.contains("enter a discount code"));
        });
    }

    @Test
    public void validateDiscount_emptyCode_returnsFailure() {
        discountRepo.validateAndApply("  ", "event1", 100.0, result -> {
            assertFalse(result.valid);
            assertTrue(result.message.contains("enter a discount code"));
        });
    }

    @Test
    public void discountResult_success_hasCorrectFields() {
        DiscountRepository.DiscountResult result =
                DiscountRepository.DiscountResult.success(
                        "percentage", 20.0, 80.0, "disc123");
        assertTrue(result.valid);
        assertEquals("percentage", result.discountType);
        assertEquals(20.0, result.discountValue, 0.01);
        assertEquals(80.0, result.finalPrice, 0.01);
        assertEquals("disc123", result.discountId);
    }

    @Test
    public void discountResult_failure_hasNullFields() {
        DiscountRepository.DiscountResult result =
                DiscountRepository.DiscountResult.failure("Invalid code");
        assertFalse(result.valid);
        assertEquals("Invalid code", result.message);
        assertNull(result.discountType);
        assertEquals(0, result.discountValue, 0.01);
    }

    // ═══ #34 — Idempotency Tests ══════════════════════════════

    @Test
    public void idempotency_nullSubmissionId_returnsFailure() {
        idempotencyManager.generateTicketIdempotent(
                null, "user1", "event1", "general", "QR123",
                new IdempotencyManager.OnIdempotentResultListener() {
                    @Override
                    public void onTicketReady(String ticketId, boolean wasExisting) {
                        fail("Should not succeed with null submissionId");
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        assertTrue(errorMessage.contains("required"));
                    }
                });
    }

    @Test
    public void idempotency_emptySubmissionId_returnsFailure() {
        idempotencyManager.generateTicketIdempotent(
                "", "user1", "event1", "general", "QR123",
                new IdempotencyManager.OnIdempotentResultListener() {
                    @Override
                    public void onTicketReady(String ticketId, boolean wasExisting) {
                        fail("Should not succeed with empty submissionId");
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        assertTrue(errorMessage.contains("required"));
                    }
                });
    }

    // ═══ TKT-A — Ticket Generation Tests ══════════════════════

    @Test
    public void generateTicket_nullSubmissionId_returnsFailure() {
        ticketService.generateTicket(null,
                new TicketGenerationService.OnTicketGeneratedListener() {
                    @Override
                    public void onSuccess(String ticketId, String qrCodeData) {
                        fail("Should not succeed with null submissionId");
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        assertTrue(errorMessage.contains("required"));
                    }
                });
    }

    @Test
    public void generateTicket_emptySubmissionId_returnsFailure() {
        ticketService.generateTicket("",
                new TicketGenerationService.OnTicketGeneratedListener() {
                    @Override
                    public void onSuccess(String ticketId, String qrCodeData) {
                        fail("Should not succeed with empty submissionId");
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        assertTrue(errorMessage.contains("required"));
                    }
                });
    }

    // ═══ ORG-A — Payment QR Repository Tests ══════════════════

    @Test
    public void saveQR_nullEventId_returnsFailure() {
        paymentQRRepo.savePaymentQRUrl(null, "https://example.com/qr.jpg",
                new PaymentQRRepository.OnQRSavedListener() {
                    @Override
                    public void onSuccess() {
                        fail("Should not succeed with null eventId");
                    }

                    @Override
                    public void onFailure(String msg) {
                        assertTrue(msg.contains("Event ID is required"));
                    }
                });
    }

    @Test
    public void saveQR_nullUrl_returnsFailure() {
        paymentQRRepo.savePaymentQRUrl("event1", null,
                new PaymentQRRepository.OnQRSavedListener() {
                    @Override
                    public void onSuccess() {
                        fail("Should not succeed with null URL");
                    }

                    @Override
                    public void onFailure(String msg) {
                        assertTrue(msg.contains("Download URL is required"));
                    }
                });
    }

    @Test
    public void getQR_nullEventId_returnsNull() {
        paymentQRRepo.getPaymentQRUrl(null, url -> assertNull(url));
    }

    @Test
    public void removeQR_emptyEventId_returnsFailure() {
        paymentQRRepo.removePaymentQR("",
                new PaymentQRRepository.OnQRSavedListener() {
                    @Override
                    public void onSuccess() {
                        fail("Should not succeed");
                    }

                    @Override
                    public void onFailure(String msg) {
                        assertTrue(msg.contains("Event ID is required"));
                    }
                });
    }
}
