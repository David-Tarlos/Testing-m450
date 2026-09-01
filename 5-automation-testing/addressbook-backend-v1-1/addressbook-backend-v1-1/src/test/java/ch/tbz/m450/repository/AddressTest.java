package ch.tbz.m450.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Address (Entity)")
class AddressTest {

    private Date registrationDate;
    private Address address;

    @BeforeEach
    void setUp() {
        registrationDate = new Date(1_700_000_000_000L);
        address = new Address(1, "Anna", "Muster", "079 111 22 33", registrationDate);
    }

    @Test
    @DisplayName("Der All-Args-Konstruktor setzt alle Felder")
    void allArgsConstructorSetsEveryField() {
        assertThat(address.getId()).isEqualTo(1);
        assertThat(address.getFirstname()).isEqualTo("Anna");
        assertThat(address.getLastname()).isEqualTo("Muster");
        assertThat(address.getPhonenumber()).isEqualTo("079 111 22 33");
        assertThat(address.getRegistrationDate()).isEqualTo(registrationDate);
    }

    @Test
    @DisplayName("Der No-Args-Konstruktor liefert ein leeres Objekt (wird von JPA gebraucht)")
    void noArgsConstructorLeavesFieldsEmpty() {
        Address empty = new Address();

        assertThat(empty.getId()).isZero();
        assertThat(empty.getFirstname()).isNull();
        assertThat(empty.getLastname()).isNull();
        assertThat(empty.getPhonenumber()).isNull();
        assertThat(empty.getRegistrationDate()).isNull();
    }

    @Test
    @DisplayName("Die Setter ueberschreiben die Werte")
    void settersOverwriteValues() {
        Date newDate = new Date(1_800_000_000_000L);

        address.setId(42);
        address.setFirstname("Bert");
        address.setLastname("Beispiel");
        address.setPhonenumber("044 000 00 00");
        address.setRegistrationDate(newDate);

        assertThat(address.getId()).isEqualTo(42);
        assertThat(address.getFirstname()).isEqualTo("Bert");
        assertThat(address.getLastname()).isEqualTo("Beispiel");
        assertThat(address.getPhonenumber()).isEqualTo("044 000 00 00");
        assertThat(address.getRegistrationDate()).isEqualTo(newDate);
    }
}
