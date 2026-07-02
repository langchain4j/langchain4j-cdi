package dev.langchain4j.cdi.example.booking;

import java.util.List;

/** Response from the fraud detection AI service. Not a record because Google JSON does not support records. */
public class FraudResponse {
    private String customerName;
    private String customerSurname;
    private boolean fraudDetected;
    private List<String> bookingIds;
    private String fraudExplanation;

    /** Creates a new empty fraud response. */
    public FraudResponse() {}

    /**
     * Returns the customer's first name.
     *
     * @return the customer name
     */
    public String getCustomerName() {
        return customerName;
    }

    /**
     * Sets the customer's first name.
     *
     * @param customerName the customer name
     */
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    /**
     * Returns the customer's surname.
     *
     * @return the customer surname
     */
    public String getCustomerSurname() {
        return customerSurname;
    }

    /**
     * Sets the customer's surname.
     *
     * @param customerSurname the customer surname
     */
    public void setCustomerSurname(String customerSurname) {
        this.customerSurname = customerSurname;
    }

    /**
     * Returns whether fraud was detected.
     *
     * @return {@code true} if fraud was detected, {@code false} otherwise
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
     * Returns the list of overlapping booking IDs.
     *
     * @return the booking identifiers involved in fraud
     */
    public List<String> getBookingIds() {
        return bookingIds;
    }

    /**
     * Sets the list of overlapping booking IDs.
     *
     * @param bookingIds the booking identifiers involved in fraud
     */
    public void setBookingIds(List<String> bookingIds) {
        this.bookingIds = bookingIds;
    }

    /**
     * Returns the fraud explanation.
     *
     * @return the explanation of the detected fraud
     */
    public String getFraudExplanation() {
        return fraudExplanation;
    }

    /**
     * Sets the fraud explanation.
     *
     * @param fraudExplanation the explanation of the detected fraud
     */
    public void setFraudExplanation(String fraudExplanation) {
        this.fraudExplanation = fraudExplanation;
    }
}
