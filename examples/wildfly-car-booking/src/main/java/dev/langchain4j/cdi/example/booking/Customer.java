package dev.langchain4j.cdi.example.booking;

import java.util.Objects;

/** Represents a car rental customer. */
public class Customer {
    private String name;
    private String surname;

    public Customer() {}

    public Customer(String name, String surname) {
        this.name = name;
        this.surname = surname;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

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
