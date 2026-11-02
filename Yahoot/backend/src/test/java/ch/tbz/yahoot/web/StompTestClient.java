package ch.tbz.yahoot.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/** Ein echter STOMP-Client, wie ihn spaeter Beamer und Handy im Browser haben. */
final class StompTestClient implements AutoCloseable {

    private final StompSession session;

    private StompTestClient(StompSession session) {
        this.session = session;
    }

    static StompTestClient connect(int port) throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new JacksonJsonMessageConverter());
        StompSession session = client
                .connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() { })
                .get(5, TimeUnit.SECONDS);
        return new StompTestClient(session);
    }

    /** Abonniert ein Ziel. Weil der Server die Reihenfolge pro Sitzung einhaelt, ist es sofort aktiv. */
    Inbox subscribe(String destination) {
        Inbox inbox = new Inbox();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                inbox.messages.add((Map<String, Object>) payload);
            }
        });
        return inbox;
    }

    void send(String destination, Object payload) {
        session.send(destination, payload);
    }

    @Override
    public void close() {
        session.disconnect();
    }

    static final class Inbox {
        private final BlockingQueue<Map<String, Object>> messages = new LinkedBlockingQueue<>();

        Map<String, Object> next() throws InterruptedException {
            Map<String, Object> message = messages.poll(5, TimeUnit.SECONDS);
            assertThat(message).as("keine Nachricht innerhalb von 5 Sekunden").isNotNull();
            return message;
        }

        /** Ueberspringt Nachrichten, bis eine vom gesuchten Typ kommt. */
        Map<String, Object> next(String type) throws InterruptedException {
            Map<String, Object> message;
            do {
                message = next();
            } while (!type.equals(message.get("type")));
            return message;
        }

        boolean isEmptyFor(long millis) throws InterruptedException {
            return messages.poll(millis, TimeUnit.MILLISECONDS) == null;
        }
    }
}
