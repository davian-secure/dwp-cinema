package uk.gov.dwp.uc.pairtest;

import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;

import java.util.Arrays;
import java.util.List;

public record TicketPurchaseRequest(List<TicketTypeRequest> requests) {

    public TicketPurchaseRequest(TicketTypeRequest... requests) {
        this(requests != null ? Arrays.asList(requests) : List.of());
    }

    public int getQuantityForType(TicketTypeRequest.Type type) {
        return requests.stream()
            .filter(request -> request.getTicketType() == type)
            .mapToInt(TicketTypeRequest::getNoOfTickets)
            .sum();
    }

    public int getTotalTicketCount() {
        return requests.stream()
            .mapToInt(TicketTypeRequest::getNoOfTickets)
            .sum();
    }
}