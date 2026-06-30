package org.wildfly.ai.booking;

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
            final String bookingNumber,
            final LocalDate start,
            final LocalDate end,
            final Customer customer,
            final boolean canceled,
            final String carModel) {
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
        return this.bookingNumber;
    }

    /**
     * Returns the start date.
     *
     * @return the start date
     */
    public LocalDate getStart() {
        return this.start;
    }

    /**
     * Returns the end date.
     *
     * @return the end date
     */
    public LocalDate getEnd() {
        return this.end;
    }

    /**
     * Returns the customer.
     *
     * @return the customer
     */
    public Customer getCustomer() {
        return this.customer;
    }

    /**
     * Returns whether the booking is canceled.
     *
     * @return {@code true} if the booking is canceled
     */
    public boolean isCanceled() {
        return this.canceled;
    }

    /**
     * Returns the car model.
     *
     * @return the car model
     */
    public String getCarModel() {
        return this.carModel;
    }

    /**
     * Sets the booking number.
     *
     * @param bookingNumber the booking number
     */
    public void setBookingNumber(final String bookingNumber) {
        this.bookingNumber = bookingNumber;
    }

    /**
     * Sets the start date.
     *
     * @param start the start date
     */
    public void setStart(final LocalDate start) {
        this.start = start;
    }

    /**
     * Sets the end date.
     *
     * @param end the end date
     */
    public void setEnd(final LocalDate end) {
        this.end = end;
    }

    /**
     * Sets the customer.
     *
     * @param customer the customer
     */
    public void setCustomer(final Customer customer) {
        this.customer = customer;
    }

    /**
     * Sets whether the booking is canceled.
     *
     * @param canceled whether the booking is canceled
     */
    public void setCanceled(final boolean canceled) {
        this.canceled = canceled;
    }

    /**
     * Sets the car model.
     *
     * @param carModel the car model
     */
    public void setCarModel(final String carModel) {
        this.carModel = carModel;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + Objects.hashCode(this.bookingNumber);
        hash = 97 * hash + Objects.hashCode(this.start);
        hash = 97 * hash + Objects.hashCode(this.end);
        hash = 97 * hash + Objects.hashCode(this.customer);
        hash = 97 * hash + (this.canceled ? 1 : 0);
        hash = 97 * hash + Objects.hashCode(this.carModel);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Booking other = (Booking) obj;
        if (this.canceled != other.canceled) {
            return false;
        }
        if (!Objects.equals(this.bookingNumber, other.bookingNumber)) {
            return false;
        }
        if (!Objects.equals(this.carModel, other.carModel)) {
            return false;
        }
        if (!Objects.equals(this.start, other.start)) {
            return false;
        }
        if (!Objects.equals(this.end, other.end)) {
            return false;
        }
        return Objects.equals(this.customer, other.customer);
    }

    @Override
    public String toString() {
        return "Booking{" + "bookingNumber=" + bookingNumber + ", start=" + start + ", end=" + end + ", customer="
                + customer + ", cancelled=" + canceled + ", carModel=" + carModel + '}';
    }
}
