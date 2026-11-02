package ch.tbz.yahoot.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Gehostet liegt die App hinter einem HTTPS-Proxy: Der Browser spricht https://, die App sieht http://.
 * Trotzdem muss sie ihre eigene Adresse erkennen und fremde Seiten abweisen.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketOriginIT {

    @LocalServerPort
    private int port;

    @Test
    void ownAddressBehindAnHttpsProxyMayConnect() {
        try (HttpClient client = HttpClient.newHttpClient()) {
            WebSocket socket = client.newWebSocketBuilder()
                    .header("Origin", "https://yahoot.example")
                    .header("X-Forwarded-Proto", "https")
                    .header("X-Forwarded-Host", "yahoot.example")
                    .buildAsync(uri(), new WebSocket.Listener() { })
                    .join();

            assertThat(socket.isOutputClosed()).isFalse();
            socket.abort();
        }
    }

    @Test
    void foreignPageMayNotConnect() {
        try (HttpClient client = HttpClient.newHttpClient()) {
            assertThatThrownBy(() -> client.newWebSocketBuilder()
                    .header("Origin", "https://boese.example")
                    .header("X-Forwarded-Proto", "https")
                    .header("X-Forwarded-Host", "yahoot.example")
                    .buildAsync(uri(), new WebSocket.Listener() { })
                    .join())
                    .isInstanceOf(CompletionException.class);
        }
    }

    private URI uri() {
        return URI.create("ws://localhost:" + port + "/ws");
    }
}
