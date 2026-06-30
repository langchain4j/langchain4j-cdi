package dev.langchain4j.cdi.example.booking;

import java.util.Objects;

/** Customer information. */
public class Customer {
    private String name;
    private String surname;

    /** Creates a new customer. */
    public Customer() {
        super();
    }

    /**
     * Creates a new customer with the given name and surname.
     *
     * @param name the customer's first name
     * @param surname the customer's surname
     */
    public Customer(String name, String surname) {
        super();
        this.name = name;
        this.surname = surname;
    }

    /**
     * Returns the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the surname.
     *
     * @return the surname
     */
    public String getSurname() {
        return surname;
    }

    /**
     * Sets the surname.
     *
     * @param surname the surname to set
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
