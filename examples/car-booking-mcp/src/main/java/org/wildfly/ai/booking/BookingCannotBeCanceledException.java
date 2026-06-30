package org.wildfly.ai.booking;

/** Thrown when a booking cannot be canceled due to policy restrictions. */
public class BookingCannotBeCanceledException extends RuntimeException {

    /**
     * Creates a new exception for a non-cancelable booking.
     *
     * @param bookingNumber the booking number
     */
    public BookingCannotBeCanceledException(String bookingNumber) {
        super("Booking " + bookingNumber + " cannot be canceled");
    }
}
