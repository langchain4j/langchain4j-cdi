package dev.langchain4j.cdi.example.booking;

/** Thrown when a booking cannot be canceled. */
public class BookingCannotBeCanceledException extends RuntimeException {

    /**
     * Creates a new exception for the given booking number.
     *
     * @param bookingNumber the booking number
     */
    public BookingCannotBeCanceledException(String bookingNumber) {
        super("Booking " + bookingNumber + " cannot be canceled");
    }
}
