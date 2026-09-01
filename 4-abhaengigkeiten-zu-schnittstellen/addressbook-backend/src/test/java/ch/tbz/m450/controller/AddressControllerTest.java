package ch.tbz.m450.controller;

import ch.tbz.m450.repository.Address;
import ch.tbz.m450.service.AddressService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testet die REST-Schicht isoliert. {@code @WebMvcTest} startet nur den Web-Layer,
 * nicht die ganze Anwendung - JPA und H2 bleiben aussen vor.
 *
 * <p>Der {@link AddressService} wird mit {@code @MockitoBean} durch ein Test Double
 * ersetzt und in den Spring-Kontext gestellt. Das ist derselbe Mock-Gedanke wie in
 * {@code AddressServiceTest}, nur eine Schicht hoeher: dort wurde die Datenbank
 * weggemockt, hier die Geschaeftslogik. Geprueft wird ausschliesslich, was der
 * Controller selbst leistet - Routing, Statuscodes und das Mapping von
 * {@link Optional} auf 404.
 *
 * <p>Hinweis: {@code @MockitoBean} loest das seit Spring Boot 3.4 veraltete
 * {@code @MockBean} ab.
 */
@WebMvcTest(AddressController.class)
class AddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AddressService addressService;

    private Address meierAnna;
    private Address aebiZoe;

    @BeforeEach
    void setUp() {
        meierAnna = new Address(3, "Anna", "Meier", "079 300 00 00", new Date(3_000L));
        aebiZoe = new Address(2, "Zoe", "Aebi", "079 200 00 00", new Date(2_000L));
    }

    @Test
    @DisplayName("POST /address antwortet mit 201 und der gespeicherten Adresse")
    void createAddressReturns201() throws Exception {
        when(addressService.save(any(Address.class))).thenReturn(meierAnna);

        mockMvc.perform(post("/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(meierAnna)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.lastname").value("Meier"));

        verify(addressService).save(any(Address.class));
    }

    @Test
    @DisplayName("GET /address antwortet mit 200 und der Liste in der Reihenfolge des Service")
    void getAddressesReturns200WithList() throws Exception {
        when(addressService.getAll()).thenReturn(List.of(aebiZoe, meierAnna));

        mockMvc.perform(get("/address"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].lastname").value("Aebi"))
                .andExpect(jsonPath("$[1].lastname").value("Meier"));
    }

    @Test
    @DisplayName("GET /address liefert ein leeres Array, wenn keine Adressen da sind")
    void getAddressesReturnsEmptyArray() throws Exception {
        when(addressService.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/address"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /address/{id} antwortet mit 200, wenn die Adresse existiert")
    void getAddressReturns200WhenFound() throws Exception {
        when(addressService.getAddress(3)).thenReturn(Optional.of(meierAnna));

        mockMvc.perform(get("/address/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstname").value("Anna"));
    }

    @Test
    @DisplayName("GET /address/{id} antwortet mit 404, wenn die Adresse fehlt")
    void getAddressReturns404WhenMissing() throws Exception {
        when(addressService.getAddress(999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/address/999"))
                .andExpect(status().isNotFound());

        verify(addressService).getAddress(999);
        verify(addressService, never()).getAll();
    }

    @Test
    @DisplayName("Eine nicht-numerische Id fuehrt zu 400, der Service wird gar nicht erst gerufen")
    void nonNumericIdIsRejectedBeforeReachingTheService() throws Exception {
        mockMvc.perform(get("/address/abc"))
                .andExpect(status().isBadRequest());

        verify(addressService, never()).getAddress(org.mockito.ArgumentMatchers.anyInt());
    }
}
