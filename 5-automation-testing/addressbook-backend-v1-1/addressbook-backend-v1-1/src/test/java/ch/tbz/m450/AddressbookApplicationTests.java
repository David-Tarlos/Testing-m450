package ch.tbz.m450;

import ch.tbz.m450.controller.AddressController;
import ch.tbz.m450.repository.AddressRepository;
import ch.tbz.m450.service.AddressService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke-Test: faehrt den kompletten Context inkl. H2 hoch. Wenn hier etwas
 * kaputt geht, stimmt die Verdrahtung nicht - unabhaengig von der Fachlogik.
 */
@SpringBootTest
@DisplayName("AddressbookApplication")
class AddressbookApplicationTests {

    @Autowired
    private AddressController addressController;

    @Autowired
    private AddressService addressService;

    @Autowired
    private AddressRepository addressRepository;

    @Test
    @DisplayName("der Spring-Context startet und alle Beans sind verdrahtet")
    void contextLoads() {
        assertThat(addressController).isNotNull();
        assertThat(addressService).isNotNull();
        assertThat(addressRepository).isNotNull();
    }
}
