package dev.langchain4j.cdi.example.booking;

import java.util.List;

// Warning: Java Record not supported by Google JSON (used by LangChain4J)
/** Fraud detection response. */
public class FraudResponse {
    private String customerName;
    private String customerSurname;
    private boolean fraudDetected;
    private List<String> bookingIds;

    /** Creates a new fraud response. */
    public FraudResponse() {}

    /**
     * Returns the customer name.
     *
     * @return the customer name
     */
    public String getCustomerName() {
        return customerName;
    }

    /**
     * Sets the customer name.
     *
     * @param customerName the customer name to set
     */
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    /**
     * Returns the customer surname.
     *
     * @return the customer surname
     */
    public String getCustomerSurname() {
        return customerSurname;
    }

    /**
     * Sets the customer surname.
     *
     * @param customerSurname the customer surname to set
     */
    public void setCustomerSurname(String customerSurname) {
        this.customerSurname = customerSurname;
    }

    /**
     * Returns whether fraud was detected.
     *
     * @return {@code true} if fraud was detected
     */
    public boolean isFraudDetected() {
        return fraudDetected;
    }

    /**
     * Sets whether fraud was detected.
     *
     * @param fraudDetected {@code true} if fraud was detected
     */
    public void setFraudDetected(boolean fraudDetected) {
        this.fraudDetected = fraudDetected;
    }

    /**
     * Returns the booking IDs.
     *
     * @return the booking IDs
     */
    public List<String> getBookingIds() {
        return bookingIds;
    }

    /**
     * Sets the booking IDs.
     *
     * @param bookingIds the booking IDs to set
     */
    public void setBookingIds(List<String> bookingIds) {
        this.bookingIds = bookingIds;
    }
}
