package uk.gov.dwp.uc.pairtest;

import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;

import java.math.BigDecimal;

public class TicketCalculationService {

    public BigDecimal calculateTotalAmount(TicketPurchaseRequest request) {
        return request.requests().stream()
            .map(ticketRequest -> getPrice(ticketRequest.getTicketType()).multiply(BigDecimal.valueOf(ticketRequest.getNoOfTickets())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public long calculateTotalSeats(TicketPurchaseRequest purchaseRequest) {
        return purchaseRequest.requests().stream()
            .mapToLong(ticketRequest -> getRequiredSeats(ticketRequest.getTicketType()) * ticketRequest.getNoOfTickets())
                .sum();
    }

    private BigDecimal getPrice(TicketTypeRequest.Type type) {
        return switch (type) {
            case ADULT -> new BigDecimal("25.00");
            case CHILD -> new BigDecimal("15.00");
            case INFANT -> BigDecimal.ZERO;
        };
    }

    private long getRequiredSeats(TicketTypeRequest.Type type) {
        return switch (type) {
            case ADULT, CHILD -> 1;
            case INFANT -> 0;
        };
    }
}