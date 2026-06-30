package dev.langchain4j.cdi.example.booking;

import java.util.Objects;

/** Represents a car rental customer. */
public class Customer {
    private String name;
    private String surname;

    /** Creates a new empty customer. */
    public Customer() {}

    /**
     * Creates a new customer with the specified name and surname.
     *
     * @param name the customer's first name
     * @param surname the customer's surname
     */
    public Customer(String name, String surname) {
        this.name = name;
        this.surname = surname;
    }

    /**
     * Returns the customer's first name.
     *
     * @return the first name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the customer's first name.
     *
     * @param name the first name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the customer's surname.
     *
     * @return the surname
     */
    public String getSurname() {
        return surname;
    }

    /**
     * Sets the customer's surname.
     *
     * @param surname the surname
     */
    public void setSurname(String surname) {
        this.surname = surname;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, surname);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Customer other = (Customer) obj;
        return Objects.equals(name, other.name) && Objects.equals(surname, other.surname);
    }

    @Override
    public String toString() {
        return "Customer [name=" + name + ", surname=" + surname + "]";
    }
}
