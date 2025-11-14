package org.wildfly.ai.booking;

import java.time.LocalDate;
import java.util.Objects;

public class Booking {

    private String bookingNumber;
    private LocalDate start;
    private LocalDate end;
    private Customer customer;
    private boolean canceled = false;
    private String carModel;

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

    public String getBookingNumber() {
        return this.bookingNumber;
    }

    public LocalDate getStart() {
        return this.start;
    }

    public LocalDate getEnd() {
        return this.end;
    }

    public Customer getCustomer() {
        return this.customer;
    }

    public boolean isCanceled() {
        return this.canceled;
    }

    public String getCarModel() {
        return this.carModel;
    }

    public void setBookingNumber(final String bookingNumber) {
        this.bookingNumber = bookingNumber;
    }

    public void setStart(final LocalDate start) {
        this.start = start;
    }

    public void setEnd(final LocalDate end) {
        this.end = end;
    }

    public void setCustomer(final Customer customer) {
        this.customer = customer;
    }

    public void setCanceled(final boolean canceled) {
        this.canceled = canceled;
    }

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
