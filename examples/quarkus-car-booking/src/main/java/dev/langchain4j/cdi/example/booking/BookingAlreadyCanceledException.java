package dev.langchain4j.cdi.example.booking;

/** Thrown when a booking has already been canceled. */
public class BookingAlreadyCanceledException extends RuntimeException {

    /**
     * Creates a new exception for the given booking number.
     *
     * @param bookingNumber the booking number
     */
    public BookingAlreadyCanceledException(String bookingNumber) {
        super("Booking " + bookingNumber + " already canceled");
    }
}
