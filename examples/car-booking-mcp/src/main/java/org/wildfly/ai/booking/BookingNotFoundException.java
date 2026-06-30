package org.wildfly.ai.booking;

/** Thrown when a booking cannot be found. */
public class BookingNotFoundException extends RuntimeException {

    /**
     * Creates a new exception for a missing booking.
     *
     * @param bookingNumber the booking number
     */
    public BookingNotFoundException(String bookingNumber) {
        super("Booking " + bookingNumber + " not found");
    }
}
