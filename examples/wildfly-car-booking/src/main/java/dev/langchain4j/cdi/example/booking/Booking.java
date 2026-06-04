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

    public Booking() {}

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

    public String getBookingNumber() {
        return bookingNumber;
    }

    public void setBookingNumber(String bookingNumber) {
        this.bookingNumber = bookingNumber;
    }

    public LocalDate getStart() {
        return start;
    }

    public void setStart(LocalDate start) {
        this.start = start;
    }

    public LocalDate getEnd() {
        return end;
    }

    public void setEnd(LocalDate end) {
        this.end = end;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public String getCarModel() {
        return carModel;
    }

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
