package dev.langchain4j.cdi.example.booking;

/** Thrown when a booking is not found. */
public class BookingNotFoundException extends RuntimeException {

    /**
     * Creates a new exception for the given booking number.
     *
     * @param bookingNumber the booking number
     */
    public BookingNotFoundException(String bookingNumber) {
        super("Booking " + bookingNumber + " not found");
    }
}
