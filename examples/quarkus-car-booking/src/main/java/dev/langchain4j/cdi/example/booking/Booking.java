package dev.langchain4j.cdi.example.booking;

import java.time.LocalDate;
import java.util.Objects;

/** Car booking details. */
public class Booking {

    private String bookingNumber;
    private LocalDate start;
    private LocalDate end;
    private Customer customer;
    private boolean canceled = false;
    private String carModel;

    /** Creates a new booking. */
    public Booking() {
        super();
    }

    /**
     * Creates a new booking with the given details.
     *
     * @param bookingNumber the booking number
     * @param start the start date
     * @param end the end date
     * @param customer the customer
     * @param canceled whether the booking is canceled
     * @param carModel the car model
     */
    public Booking(
            String bookingNumber,
            LocalDate start,
            LocalDate end,
            Customer customer,
            boolean canceled,
            String carModel) {
        super();
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
     * @return the booking number
     */
    public String getBookingNumber() {
        return bookingNumber;
    }

    /**
     * Sets the booking number.
     *
     * @param bookingNumber the booking number to set
     */
    public void setBookingNumber(String bookingNumber) {
        this.bookingNumber = bookingNumber;
    }

    /**
     * Returns the start date.
     *
     * @return the start date
     */
    public LocalDate getStart() {
        return start;
    }

    /**
     * Sets the start date.
     *
     * @param start the start date to set
     */
    public void setStart(LocalDate start) {
        this.start = start;
    }

    /**
     * Returns the end date.
     *
     * @return the end date
     */
    public LocalDate getEnd() {
        return end;
    }

    /**
     * Sets the end date.
     *
     * @param end the end date to set
     */
    public void setEnd(LocalDate end) {
        this.end = end;
    }

    /**
     * Returns the customer.
     *
     * @return the customer
     */
    public Customer getCustomer() {
        return customer;
    }

    /**
     * Sets the customer.
     *
     * @param customer the customer to set
     */
    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    /**
     * Returns whether the booking is canceled.
     *
     * @return {@code true} if the booking is canceled
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Sets whether the booking is canceled.
     *
     * @param canceled {@code true} if the booking is canceled
     */
    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    /**
     * Returns the car model.
     *
     * @return the car model
     */
    public String getCarModel() {
        return carModel;
    }

    /**
     * Sets the car model.
     *
     * @param carModel the car model to set
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
