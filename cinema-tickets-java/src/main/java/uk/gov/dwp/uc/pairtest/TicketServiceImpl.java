package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketServiceImpl implements TicketService {
    /**
     * Should only have private methods other than the one below.
     */

    private final SeatReservationService seatReservationService;
    private final TicketPaymentService ticketPaymentService;
    private final TicketPurchaseValidator purchaseValidator;
    private final TicketCalculationService calculationService;

    public TicketServiceImpl(SeatReservationService seatReservationService, TicketPaymentService ticketPaymentService, TicketPurchaseValidator purchaseValidator, TicketCalculationService calculationService) {
        this.seatReservationService = seatReservationService;
        this.ticketPaymentService = ticketPaymentService;
        this.purchaseValidator = purchaseValidator;
        this.calculationService = calculationService;
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketRequests) throws InvalidPurchaseException {

        var purchaseRequest = new TicketPurchaseRequest(ticketRequests);

        purchaseValidator.validate(accountId, purchaseRequest);

        var totalAmount = calculationService.calculateTotalAmount(purchaseRequest);
        long totalSeats = calculationService.calculateTotalSeats(purchaseRequest);

        ticketPaymentService.makePayment(accountId, totalAmount.intValue());
        seatReservationService.reserveSeat(accountId, (int)totalSeats);
    }

}
