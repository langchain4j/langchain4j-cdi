package dev.langchain4j.cdi.example.booking;

import java.time.LocalDate;
import java.util.Objects;

/** Represents a car rental booking. */
public class Booking {

    private String bookingNumber;
    private LocalDate start;
    private LocalDate end;
    private Customer customer;
    private boolean canceled = false;
    private String carModel;

    /** Creates a new empty booking. */
    public Booking() {}

    /**
     * Creates a new booking with the specified details.
     *
     * @param bookingNumber the unique booking identifier
     * @param start the start date of the rental period
     * @param end the end date of the rental period
     * @param customer the customer who made the booking
     * @param canceled whether the booking is canceled
     * @param carModel the model of the rented car
     */
    public Booking(
            String bookingNumber,
            LocalDate start,
            LocalDate end,
            Customer customer,
            boolean canceled,
            String carModel) {
        this.bookingNumber = bookingNumber;
        this.start = start;
        this.end = end;
        this.customer = customer;
        this.canceled = canceled;
        this.carModel = carModel;
    }

    /**
     * Returns the booking number.
     *
     * @return the unique booking identifier
     */
    public String getBookingNumber() {
        return bookingNumber;
    }

    /**
     * Sets the booking number.
     *
     * @param bookingNumber the unique booking identifier
     */
    public void setBookingNumber(String bookingNumber) {
        this.bookingNumber = bookingNumber;
    }

    /**
     * Returns the start date of the rental period.
     *
     * @return the start date
     */
    public LocalDate getStart() {
        return start;
    }

    /**
     * Sets the start date of the rental period.
     *
     * @param start the start date
     */
    public void setStart(LocalDate start) {
        this.start = start;
    }

    /**
     * Returns the end date of the rental period.
     *
     * @return the end date
     */
    public LocalDate getEnd() {
        return end;
    }

    /**
     * Sets the end date of the rental period.
     *
     * @param end the end date
     */
    public void setEnd(LocalDate end) {
        this.end = end;
    }

    /**
     * Returns the customer who made the booking.
     *
     * @return the customer
     */
    public Customer getCustomer() {
        return customer;
    }

    /**
     * Sets the customer who made the booking.
     *
     * @param customer the customer
     */
    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    /**
     * Returns whether the booking is canceled.
     *
     * @return {@code true} if canceled, {@code false} otherwise
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Sets the canceled status of the booking.
     *
     * @param canceled {@code true} to mark as canceled
     */
    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    /**
     * Returns the car model.
     *
     * @return the car model name
     */
    public String getCarModel() {
        return carModel;
    }

    /**
     * Sets the car model.
     *
     * @param carModel the car model name
     */
    public void setCarModel(String carModel) {
        this.carModel = carModel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookingNumber, canceled, carModel, customer, end, start);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Booking other = (Booking) obj;
        return Objects.equals(bookingNumber, other.bookingNumber)
                && canceled == other.canceled
                && Objects.equals(carModel, other.carModel)
                && Objects.equals(customer, other.customer)
                && Objects.equals(end, other.end)
                && Objects.equals(start, other.start);
    }

    @Override
    public String toString() {
        return "Booking [bookingNumber=" + bookingNumber + ", start=" + start + ", end=" + end + ", customer="
                + customer + ", canceled=" + canceled + ", carModel=" + carModel + "]";
    }
}
