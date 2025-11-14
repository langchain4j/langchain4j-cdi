package org.wildfly.ai.booking;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;
import org.wildfly.mcp.api.Prompt;
import org.wildfly.mcp.api.PromptArg;
import org.wildfly.mcp.api.PromptMessage;
import org.wildfly.mcp.api.TextContent;
import org.wildfly.mcp.api.Tool;
import org.wildfly.mcp.api.ToolArg;

public class BookingService {

    private static final Logger log = Logger.getLogger(BookingService.class.getName());
    // Pseudo database
    private static final Map<String, Booking> BOOKINGS = new HashMap<>();

    static {
        // James Bond: hero customer!
        BOOKINGS.put(
                "123-456",
                new Booking(
                        "123-456",
                        LocalDate.now().plusDays(1),
                        LocalDate.now().plusDays(7),
                        new Customer("James", "Bond"),
                        false,
                        "Aston Martin")); // Not cancelable: too late
        BOOKINGS.put(
                "234-567",
                new Booking(
                        "234-567",
                        LocalDate.now().plusDays(10),
                        LocalDate.now().plusDays(12),
                        new Customer("James", "Bond"),
                        false,
                        "Renault")); // Not cancelable: too short
        BOOKINGS.put(
                "345-678",
                new Booking(
                        "345-678",
                        LocalDate.now().plusDays(14),
                        LocalDate.now().plusDays(20),
                        new Customer("James", "Bond"),
                        false,
                        "Porsche")); // Cancelable
        // Emilio Largo: villain frauder!
        BOOKINGS.put(
                "456-789",
                new Booking(
                        "456-789",
                        LocalDate.now().plusDays(10),
                        LocalDate.now().plusDays(20),
                        new Customer("Emilio", "Largo"),
                        false,
                        "Porsche")); // Cancelable
        BOOKINGS.put(
                "567-890",
                new Booking(
                        "567-890",
                        LocalDate.now().plusDays(11),
                        LocalDate.now().plusDays(16),
                        new Customer("Emilio", "Largo"),
                        false,
                        "BMW")); // Cancelable
    }

    // Simulate database accesses
    private Booking checkBookingExists(String bookingNumber, String name, String surname) {
        Booking booking = BOOKINGS.get(bookingNumber);
        if (booking == null
                || !booking.getCustomer().getName().equals(name)
                || !booking.getCustomer().getSurname().equals(surname)) {
            throw new BookingNotFoundException(bookingNumber);
        }
        return booking;
    }

    @Tool(description = "Get booking details given a booking id and customer name and surname")
    public Booking getBookingDetails(
            @ToolArg(description = "The booking id composed of three digits followed by a minus then three digits")
                    String bookingNumber,
            @ToolArg(description = "The name of the customer") String name,
            @ToolArg(description = "The surname of the customer") String surname) {
        log.info("DEMO: Calling Tool-getBookingDetails: " + bookingNumber + " and customer: " + name + " " + surname);
        return checkBookingExists(bookingNumber, name, surname);
    }

    @Tool(description = "Get All bookings")
    public Collection<Booking> getAllBookingDetails() {
        log.info("DEMO: Calling Tool-getAllBookingDetails");
        return BOOKINGS.values();
    }

    @Tool(description = "Get all booking ids for a customer given his name and surname")
    public List<String> getBookingsForCustomer(
            @ToolArg(description = "The name of the customer") String name,
            @ToolArg(description = "The surname of the customer") String surname) {
        log.info("DEMO: Calling Tool-getBookingsForCustomer: " + name + " " + surname);
        Customer customer = new Customer(name, surname);
        return BOOKINGS.values().stream()
                .filter(booking -> booking.getCustomer().equals(customer))
                .map(Booking::getBookingNumber)
                .collect(Collectors.toList());
    }

    public void checkCancelPolicy(Booking booking) {
        // Reservations can be cancelled up to 7 days prior to the start of the booking
        // period
        if (LocalDate.now().plusDays(7).isAfter(booking.getStart())) {
            throw new BookingCannotBeCanceledException(booking.getBookingNumber() + " Too late");
        }
        // If the booking period is less than 3 days, cancellations are not permitted.
        if (booking.getEnd().compareTo(booking.getStart().plusDays(3)) < 0) {
            throw new BookingCannotBeCanceledException(booking.getBookingNumber() + " Too short");
        }
    }

    @Tool(description = "Cancel a booking given its booking number and customer name and surname")
    public Booking cancelBooking(
            @ToolArg(description = "The booking id composed of three digits followed by a minus then three digits")
                    String bookingNumber,
            @ToolArg(description = "The name of the customer") String name,
            @ToolArg(description = "The surname of the customer") String surname) {
        log.info("DEMO: Calling Tool-cancelBooking " + bookingNumber + " for customer: " + name + " " + surname);
        Booking booking = checkBookingExists(bookingNumber, name, surname);
        if (booking.isCanceled()) {
            throw new BookingCannotBeCanceledException(bookingNumber);
        }
        checkCancelPolicy(booking);
        booking.setCanceled(true);
        return booking;
    }

    @Prompt(name = "detect-fraud-for-customer", description = "Detect fraud for customer.")
    public PromptMessage detectFraudForCustomer(
            @PromptArg(name = "name", description = "User name.", required = true) String name,
            @PromptArg(name = "surname", description = "User surname.", required = true) String surname) {
        String prompt = """
            Your task is to detect whether a fraud was committed for the customer {{name}} {{surname}}.

            To detect a fraud, perform the following actions:
            1 - Retrieve all bookings for the customer with name {{name}} and surname {{surname}}.
            2 - If there are no bookings, return the fraud status as 'false'.
            3 - Otherwise, Determine if there is an overlap between several bookings.
            4 - If there is an overlap, a fraud is detected.
            5 - If a fraud is detected, return the fraud status and the bookings that overlap.

            A booking overlap (and hence a fraud) occurs when there are several bookings for a given date.
            For instance:
            -there is no overlap if a given customer has the following bookings:
                    - Booking number 345-678 with the period from 2024-03-25 to 2024-03-31.
                    - Booking number 234-567 with the period from 2024-03-21 to 2024-03-23.
            -there is an overlap if a given customer has the following bookings:
                    - Booking number 456-789 with the period from 2024-03-21 to 2024-03-31.
                    - Booking number 567-890 with the period from 2024-03-22 to 2024-03-27.

            Answer with the following information in a valid JSON document:
            - the customer-name key set to {{name}}
            - the customer-surname key set to {{surname}}
            - the fraud-detected key set to 'true' or 'false'
            - in case of fraud, the explanation of the fraud in the fraud-explanation key
            - in case of fraud, the reservation ids that overlap.
            You must respond in a valid JSON format.

            You must not wrap JSON response in backticks, markdown, or in any other way, but return it as plain text.
            """;
        prompt = prompt.replaceAll("\\{\\{name\\}\\}", name);
        prompt = prompt.replaceAll("\\{\\{surname\\}\\}", surname);
        return PromptMessage.withUserRole(new TextContent(prompt));
    }
}
