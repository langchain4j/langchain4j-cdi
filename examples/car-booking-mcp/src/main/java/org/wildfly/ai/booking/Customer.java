package org.wildfly.ai.booking;

import java.util.Objects;

/** Customer information. */
public class Customer {

    private String name;
    private String surname;

    /**
     * Creates a new customer.
     *
     * @param name the customer's first name
     * @param surname the customer's surname
     */
    public Customer(final String name, final String surname) {
        this.name = name;
        this.surname = surname;
    }

    /**
     * Returns the name.
     *
     * @return the customer's first name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the surname.
     *
     * @return the customer's surname
     */
    public String getSurname() {
        return this.surname;
    }

    /**
     * Sets the name.
     *
     * @param name the customer's first name
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Sets the surname.
     *
     * @param surname the customer's surname
     */
    public void setSurname(final String surname) {
        this.surname = surname;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + Objects.hashCode(this.name);
        hash = 23 * hash + Objects.hashCode(this.surname);
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
        final Customer other = (Customer) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return Objects.equals(this.surname, other.surname);
    }

    @Override
    public String toString() {
        return "Customer{" + "name=" + name + ", surname=" + surname + '}';
    }
}
