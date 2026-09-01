package ch.tbz.m450;

import ch.tbz.m450.controller.AddressController;
import ch.tbz.m450.repository.AddressRepository;
import ch.tbz.m450.service.AddressService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Smoke-Test der gesamten Anwendung: hier laufen Spring, JPA und H2 wirklich.
 * Faellt eine Verdrahtung auseinander, scheitert dieser Test - unabhaengig davon,
 * ob alle Unit-Tests gruen sind.
 */
@SpringBootTest
class AddressbookApplicationTests {

    @Autowired
    private AddressController addressController;

    @Autowired
    private AddressService addressService;

    @Autowired
    private AddressRepository addressRepository;

    @Test
    @DisplayName("Der Spring-Kontext startet und verdrahtet alle drei Schichten")
    void contextLoads() {
        assertNotNull(addressController);
        assertNotNull(addressService);
        assertNotNull(addressRepository);
    }
}
