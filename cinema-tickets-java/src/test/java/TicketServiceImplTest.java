import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.TicketCalculationService;
import uk.gov.dwp.uc.pairtest.TicketPurchaseRequest;
import uk.gov.dwp.uc.pairtest.TicketPurchaseValidator;
import uk.gov.dwp.uc.pairtest.TicketServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock
    private TicketPaymentService ticketPaymentService;

    @Mock
    private SeatReservationService seatReservationService;

    @Mock
    private TicketPurchaseValidator purchaseValidator;

    @Mock
    private TicketCalculationService calculationService;

    @InjectMocks
    private TicketServiceImpl ticketService;

    private static final Long VALID_ACCOUNT_ID = 1L;

    @Test
    @DisplayName("Valid purchase validates first, then calculates, pays, and reserves seats")
    void shouldOrchestrateValidTicketPurchaseInOrder() {
        var adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        var childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);

        when(calculationService.calculateTotalAmount(any(TicketPurchaseRequest.class)))
                .thenReturn(BigDecimal.valueOf(65));
        when(calculationService.calculateTotalSeats(any(TicketPurchaseRequest.class)))
                .thenReturn(3L);

        ticketService.purchaseTickets(VALID_ACCOUNT_ID, adultRequest, childRequest);

        var inOrder = inOrder(
                purchaseValidator,
                calculationService,
                ticketPaymentService,
                seatReservationService
        );

        inOrder.verify(purchaseValidator).validate(eq(VALID_ACCOUNT_ID), any(TicketPurchaseRequest.class));
        inOrder.verify(calculationService).calculateTotalAmount(any(TicketPurchaseRequest.class));
        inOrder.verify(calculationService).calculateTotalSeats(any(TicketPurchaseRequest.class));
        inOrder.verify(ticketPaymentService).makePayment(VALID_ACCOUNT_ID, 65);
        inOrder.verify(seatReservationService).reserveSeat(VALID_ACCOUNT_ID, 3);
    }

    @Test
    @DisplayName("Stops immediately without taking payment or reserving seats when validation fails")
    void shouldAbortWhenValidationFails() {
        var childOnlyRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);

        doThrow(new InvalidPurchaseException("Child tickets require an adult"))
                .when(purchaseValidator)
                .validate(eq(VALID_ACCOUNT_ID), any(TicketPurchaseRequest.class));

        assertThrows(InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(VALID_ACCOUNT_ID, childOnlyRequest));

        verifyNoInteractions(calculationService, ticketPaymentService, seatReservationService);
    }
}