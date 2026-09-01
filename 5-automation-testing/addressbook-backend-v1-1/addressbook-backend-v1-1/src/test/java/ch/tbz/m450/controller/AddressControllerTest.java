package ch.tbz.m450.controller;

import ch.tbz.m450.repository.Address;
import ch.tbz.m450.service.AddressService;
import ch.tbz.m450.util.AddressComparator.SortDirection;
import ch.tbz.m450.util.AddressComparator.SortField;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testet nur die HTTP-Schicht: der Service ist gemockt, MockMvc laeuft im
 * standalone-Modus (kein Spring-Context, keine Datenbank).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AddressController (Service gemockt)")
class AddressControllerTest {

    @Mock
    private AddressService addressService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private Address anna;
    private Address bert;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AddressController(addressService)).build();
        objectMapper = new ObjectMapper();

        anna = new Address(3, "Anna", "Muster", "079 300 00 00", null);
        bert = new Address(1, "Bert", "Beispiel", "044 100 00 00", null);
    }

    @Test
    @DisplayName("POST /address antwortet mit 201 und der gespeicherten Adresse")
    void createAddressReturns201() throws Exception {
        when(addressService.save(any(Address.class))).thenReturn(anna);

        mockMvc.perform(post("/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(anna)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.firstname").value("Anna"))
                .andExpect(jsonPath("$.lastname").value("Muster"));

        verify(addressService).save(any(Address.class));
    }

    @Test
    @DisplayName("GET /address antwortet mit 200 und der sortierten Liste")
    void getAddressesReturns200() throws Exception {
        when(addressService.getAll(SortField.LASTNAME, SortDirection.ASC)).thenReturn(List.of(bert, anna));

        mockMvc.perform(get("/address"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].lastname").value("Beispiel"))
                .andExpect(jsonPath("$[1].lastname").value("Muster"));
    }

    @Test
    @DisplayName("GET /address reicht sortBy und direction an den Service durch")
    void getAddressesPassesSortParameters() throws Exception {
        when(addressService.getAll(SortField.FIRSTNAME, SortDirection.DESC)).thenReturn(List.of(anna, bert));

        mockMvc.perform(get("/address").param("sortBy", "FIRSTNAME").param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstname").value("Anna"));

        verify(addressService).getAll(SortField.FIRSTNAME, SortDirection.DESC);
    }

    @Test
    @DisplayName("GET /address mit unbekanntem sortBy antwortet mit 400")
    void getAddressesRejectsUnknownSortField() throws Exception {
        mockMvc.perform(get("/address").param("sortBy", "GIBTSNICHT"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /address/{id} antwortet mit 200, wenn die Adresse existiert")
    void getAddressReturns200WhenFound() throws Exception {
        when(addressService.getAddress(3)).thenReturn(Optional.of(anna));

        mockMvc.perform(get("/address/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.phonenumber").value("079 300 00 00"));
    }

    @Test
    @DisplayName("GET /address/{id} antwortet mit 404, wenn die Adresse fehlt")
    void getAddressReturns404WhenMissing() throws Exception {
        when(addressService.getAddress(999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/address/999"))
                .andExpect(status().isNotFound());

        verify(addressService).getAddress(999);
    }
}
