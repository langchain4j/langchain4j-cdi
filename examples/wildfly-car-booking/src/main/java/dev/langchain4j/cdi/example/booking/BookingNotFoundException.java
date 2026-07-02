package dev.langchain4j.cdi.example.booking;

/** Thrown when a booking cannot be found by its number. */
public class BookingNotFoundException extends RuntimeException {

    /**
     * Creates a new exception for a missing booking.
     *
     * @param bookingNumber the booking number that was not found
     */
    public BookingNotFoundException(String bookingNumber) {
        super("Booking " + bookingNumber + " not found");
    }
}
