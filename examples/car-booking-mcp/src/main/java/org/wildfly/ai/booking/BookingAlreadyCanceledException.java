package org.wildfly.ai.booking;

/** Thrown when attempting to cancel a booking that has already been canceled. */
public class BookingAlreadyCanceledException extends RuntimeException {

    /**
     * Creates a new exception for an already canceled booking.
     *
     * @param bookingNumber the booking number
     */
    public BookingAlreadyCanceledException(String bookingNumber) {
        super("Booking " + bookingNumber + " already canceled");
    }
}
