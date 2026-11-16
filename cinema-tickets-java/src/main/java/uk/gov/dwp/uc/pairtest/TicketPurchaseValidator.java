package uk.gov.dwp.uc.pairtest;

import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketPurchaseValidator {

    private static final int MAX_TICKETS = 25;

    public void validate(Long accountId, TicketPurchaseRequest purchaseRequest) {
        validateAccountId(accountId);
        validateRequestNotNull(purchaseRequest);
        validateQuantities(purchaseRequest);
        validateTotalLimit(purchaseRequest);
        validateAdultRules(purchaseRequest);
    }

    private void validateAccountId(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new InvalidPurchaseException("Account ID must be non-null and greater than 0");
        }
    }

    private void validateRequestNotNull(TicketPurchaseRequest purchaseRequest) {
        if (purchaseRequest == null || purchaseRequest.requests().isEmpty()) {
            throw new InvalidPurchaseException("Ticket purchase request cannot be null or empty");
        }
    }

    private void validateQuantities(TicketPurchaseRequest purchaseRequest) {
        for (var request : purchaseRequest.requests()) {
            if (request.getNoOfTickets() < 0) {
                throw new InvalidPurchaseException("Ticket quantity cannot be negative");
            }
        }

        if (purchaseRequest.getTotalTicketCount() == 0) {
            throw new InvalidPurchaseException("Cannot purchase 0 tickets.");
        }
    }

    private void validateTotalLimit(TicketPurchaseRequest purchaseRequest) {
        if (purchaseRequest.getTotalTicketCount() > MAX_TICKETS) {
            throw new InvalidPurchaseException("Cannot purchase more than " + MAX_TICKETS + " tickets at a time");
        }
    }

    private void validateAdultRules(TicketPurchaseRequest purchaseRequest) {
        int adultCount = purchaseRequest.getQuantityForType(TicketTypeRequest.Type.ADULT);
        int childCount = purchaseRequest.getQuantityForType(TicketTypeRequest.Type.CHILD);
        int infantCount = purchaseRequest.getQuantityForType(TicketTypeRequest.Type.INFANT);

        if ((childCount > 0 || infantCount > 0) && adultCount == 0) {
            throw new InvalidPurchaseException("Child or Infant tickets cannot be purchased without an Adult ticket");
        }

        if (infantCount > adultCount) {
            throw new InvalidPurchaseException("Infants must sit on an adult's lap. Infant count cannot exceed Adult count");
        }
    }
}
