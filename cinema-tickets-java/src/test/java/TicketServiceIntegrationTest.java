import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.TicketCalculationService;
import uk.gov.dwp.uc.pairtest.TicketPurchaseValidator;
import uk.gov.dwp.uc.pairtest.TicketServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TicketServiceIntegrationTest {

    @Mock
    private TicketPaymentService ticketPaymentService;

    @Mock
    private SeatReservationService seatReservationService;

    private TicketServiceImpl ticketService;

    private static final Long VALID_ACCOUNT_ID = 1L;

    @BeforeEach
    void setUp() {
        var purchaseValidator = new TicketPurchaseValidator();
        var calculationService = new TicketCalculationService();

        ticketService = new TicketServiceImpl(
                seatReservationService,
                ticketPaymentService,
                purchaseValidator,
                calculationService
        );
    }

    @Nested
    @DisplayName("Valid Purchases")
    class ValidPurchases {

        @Test
        @DisplayName("Should charge £25 per Adult and reserve 1 seat each")
        void shouldPurchaseAdultTickets() {
            ticketService.purchaseTickets(VALID_ACCOUNT_ID, new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 3));

            verify(ticketPaymentService).makePayment(VALID_ACCOUNT_ID, 75);
            verify(seatReservationService).reserveSeat(VALID_ACCOUNT_ID, 3);
        }

        @Test
        @DisplayName("Should charge £25 for Adult, £15 for Child, £0 for Infant, and exclude Infants from seats")
        void shouldCalculateMixedTicketsCorrectly() {
            ticketService.purchaseTickets(
                    VALID_ACCOUNT_ID,
                    new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2),
                    new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2),
                    new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1)
            );

            verify(ticketPaymentService).makePayment(VALID_ACCOUNT_ID, 80);
            verify(seatReservationService).reserveSeat(VALID_ACCOUNT_ID, 4);
        }

        @Test
        @DisplayName("Should allow purchasing up to the 25 ticket limit including Infants")
        void shouldAllowMaximum25Tickets() {
            ticketService.purchaseTickets(
                    VALID_ACCOUNT_ID,
                    new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 10),
                    new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 10),
                    new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 5)
            );

            verify(ticketPaymentService).makePayment(VALID_ACCOUNT_ID, 400);
            verify(seatReservationService).reserveSeat(VALID_ACCOUNT_ID, 20);
        }
    }

    @Nested
    @DisplayName("Invalid Purchases")
    class InvalidPurchases {

        @ParameterizedTest
        @NullSource
        @ValueSource(longs = {0L, -1L})
        @DisplayName("Should reject invalid account ID")
        void shouldRejectInvalidAccountIds(Long invalidAccountId) {
            var invalidPurchaseException = assertThrows(InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(invalidAccountId, new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1)));

            assertEquals("Account ID must be non-null and greater than 0", invalidPurchaseException.getMessage());
            verifyNoInteractions(ticketPaymentService, seatReservationService);
        }

        @Test
        @DisplayName("Should reject Child tickets without an Adult")
        void shouldRejectChildWithoutAdult() {
            var invalidPurchaseException = assertThrows(InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(VALID_ACCOUNT_ID, new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1)));

            assertEquals("Child or Infant tickets cannot be purchased without an Adult ticket", invalidPurchaseException.getMessage());
            verifyNoInteractions(ticketPaymentService, seatReservationService);
        }

        @Test
        @DisplayName("Should reject Infant tickets without an Adult")
        void shouldRejectInfantWithoutAdult() {
            var invalidPurchaseException = assertThrows(InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(VALID_ACCOUNT_ID, new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1)));

            assertEquals("Child or Infant tickets cannot be purchased without an Adult ticket", invalidPurchaseException.getMessage());
            verifyNoInteractions(ticketPaymentService, seatReservationService);
        }

        @Test
        @DisplayName("should reject more infants than adults")
        void shouldRejectMoreInfantsThanAdults() {
            var invalidPurchaseException = assertThrows(
                    InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(
                            VALID_ACCOUNT_ID,
                            new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1),
                            new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2)
                    )
            );

            assertEquals(
                    "Infants must sit on an adult's lap. Infant count cannot exceed Adult count",
                    invalidPurchaseException.getMessage()
            );
            verifyNoInteractions(ticketPaymentService, seatReservationService);
        }

        @Test
        @DisplayName("Should reject purchases exceeding 25 total tickets")
        void shouldRejectMoreThan25Tickets() {
            var invalidPurchaseException = assertThrows(InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(
                            VALID_ACCOUNT_ID,
                            new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 20),
                            new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 6)
                    ));

            assertEquals("Cannot purchase more than 25 tickets at a time", invalidPurchaseException.getMessage());
            verifyNoInteractions(ticketPaymentService, seatReservationService);
        }

        @Test
        @DisplayName("Should reject empty or null ticket requests")
        void shouldRejectEmptyOrNullRequests() {
            var emptyException = assertThrows(InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(VALID_ACCOUNT_ID));
            var nullException = assertThrows(InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(VALID_ACCOUNT_ID, (TicketTypeRequest[]) null));

            assertEquals("Ticket purchase request cannot be null or empty", emptyException.getMessage());
            assertEquals("Ticket purchase request cannot be null or empty", nullException.getMessage());
            verifyNoInteractions(ticketPaymentService, seatReservationService);
        }
    }
}