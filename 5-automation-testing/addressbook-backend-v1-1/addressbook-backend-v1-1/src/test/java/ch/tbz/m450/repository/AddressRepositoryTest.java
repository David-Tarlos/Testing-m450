package ch.tbz.m450.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hier laeuft H2 bewusst mit: der Test prueft, ob das Mapping der Entity stimmt
 * und JpaRepository die Adressen wirklich speichert und wiederfindet.
 */
@DataJpaTest
@DisplayName("AddressRepository (mit H2)")
class AddressRepositoryTest {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Address anna;
    private Address bert;

    @BeforeEach
    void setUp() {
        anna = new Address(3, "Anna", "Muster", "079 300 00 00", date(2023));
        bert = new Address(1, "Bert", "Beispiel", "044 100 00 00", date(2021));
    }

    private static Date date(int year) {
        return Date.from(LocalDate.of(year, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    @Test
    @DisplayName("speichert eine Adresse und findet sie ueber die id wieder")
    void savesAndFindsById() {
        addressRepository.save(anna);
        entityManager.flush();
        entityManager.clear();

        Optional<Address> found = addressRepository.findById(3);

        assertThat(found).isPresent();
        assertThat(found.get().getFirstname()).isEqualTo("Anna");
        assertThat(found.get().getLastname()).isEqualTo("Muster");
        assertThat(found.get().getPhonenumber()).isEqualTo("079 300 00 00");
        // Hibernate liefert eine java.sql.Timestamp zurueck - deshalb ueber den Zeitwert
        // vergleichen und nicht ueber equals()
        assertThat(found.get().getRegistrationDate()).hasSameTimeAs(date(2023));
    }

    @Test
    @DisplayName("findAll liefert alle gespeicherten Adressen")
    void findAllReturnsEverything() {
        addressRepository.saveAll(List.of(anna, bert));
        entityManager.flush();

        assertThat(addressRepository.findAll())
                .extracting(Address::getId)
                .containsExactlyInAnyOrder(1, 3);
    }

    @Test
    @DisplayName("findById gibt Optional.empty fuer eine unbekannte id")
    void findByIdReturnsEmptyForUnknownId() {
        assertThat(addressRepository.findById(999)).isEmpty();
    }

    @Test
    @DisplayName("save mit bestehender id aktualisiert den Datensatz, statt einen zweiten anzulegen")
    void saveWithExistingIdUpdates() {
        addressRepository.save(anna);
        entityManager.flush();

        addressRepository.save(new Address(3, "Anna-Maria", "Muster", "079 300 00 00", date(2023)));
        entityManager.flush();
        entityManager.clear();

        assertThat(addressRepository.findAll()).hasSize(1);
        assertThat(addressRepository.findById(3))
                .get()
                .extracting(Address::getFirstname)
                .isEqualTo("Anna-Maria");
    }
}
