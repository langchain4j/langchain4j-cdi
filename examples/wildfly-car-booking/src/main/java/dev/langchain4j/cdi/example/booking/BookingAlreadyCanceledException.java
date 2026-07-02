package dev.langchain4j.cdi.example.booking;

/** Thrown when attempting to cancel a booking that has already been canceled. */
public class BookingAlreadyCanceledException extends RuntimeException {

    /**
     * Creates a new exception for an already canceled booking.
     *
     * @param bookingNumber the booking number that was already canceled
     */
    public BookingAlreadyCanceledException(String bookingNumber) {
        super("Booking " + bookingNumber + " already canceled");
    }
}
