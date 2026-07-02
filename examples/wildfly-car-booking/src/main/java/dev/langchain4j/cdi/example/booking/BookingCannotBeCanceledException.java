package dev.langchain4j.cdi.example.booking;

/** Thrown when a booking cannot be canceled due to policy restrictions. */
public class BookingCannotBeCanceledException extends RuntimeException {

    /**
     * Creates a new exception for a booking that cannot be canceled.
     *
     * @param bookingNumber the booking number that cannot be canceled
     */
    public BookingCannotBeCanceledException(String bookingNumber) {
        super("Booking " + bookingNumber + " cannot be canceled");
    }
}
