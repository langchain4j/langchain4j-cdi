package dev.langchain4j.cdi.example.booking;

import java.util.List;

// Not a record because Google JSON (used by LangChain4J) does not support records.
public class FraudResponse {
    private String customerName;
    private String customerSurname;
    private boolean fraudDetected;
    private List<String> bookingIds;
    private String fraudExplanation;

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerSurname() {
        return customerSurname;
    }

    public void setCustomerSurname(String customerSurname) {
        this.customerSurname = customerSurname;
    }

    public boolean isFraudDetected() {
        return fraudDetected;
    }

    public void setFraudDetected(boolean fraudDetected) {
        this.fraudDetected = fraudDetected;
    }

    public List<String> getBookingIds() {
        return bookingIds;
    }

    public void setBookingIds(List<String> bookingIds) {
        this.bookingIds = bookingIds;
    }

    public String getFraudExplanation() {
        return fraudExplanation;
    }

    public void setFraudExplanation(String fraudExplanation) {
        this.fraudExplanation = fraudExplanation;
    }
}
