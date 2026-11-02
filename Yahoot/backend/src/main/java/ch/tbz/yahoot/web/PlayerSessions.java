package ch.tbz.yahoot.web;

import ch.tbz.yahoot.game.GameRuleException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Merkt sich, welcher Spieler hinter einer WebSocket-Sitzung steckt. So kann niemand im Namen eines
 * anderen antworten oder Karten spielen. Mit dem Token findet ein Handy nach einem Verbindungsabbruch
 * zurück in sein Spiel, weil die Sitzung dabei wechselt.
 */
@Component
class PlayerSessions {

    private final SecureRandom random = new SecureRandom();
    private final Map<String, PlayerRef> bySession = new ConcurrentHashMap<>();
    private final Map<String, PlayerRef> byToken = new ConcurrentHashMap<>();

    /** Bindet den Spieler an die Sitzung und gibt das Token für den Rejoin zurück. */
    String register(String sessionId, String code, String name) {
        PlayerRef ref = new PlayerRef(normalize(code), name);
        bySession.put(sessionId, ref);
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        byToken.put(token, ref);
        return token;
    }

    /** Bindet die neue Sitzung an den Spieler mit diesem Token und gibt seinen Namen zurück. */
    String rejoin(String sessionId, String code, String token) {
        PlayerRef ref = token == null ? null : byToken.get(token);
        if (ref == null || !ref.code().equals(normalize(code))) {
            throw new GameRuleException("Konnte nicht wieder verbinden, bitte neu beitreten");
        }
        bySession.put(sessionId, ref);
        return ref.name();
    }

    String playerIn(String sessionId, String code) {
        PlayerRef ref = bySession.get(sessionId);
        if (ref == null || !ref.code().equals(normalize(code))) {
            throw new GameRuleException("Du bist diesem Spiel noch nicht beigetreten");
        }
        return ref.name();
    }

    @EventListener
    void onDisconnect(SessionDisconnectEvent event) {
        bySession.remove(event.getSessionId());
    }

    private static String normalize(String code) {
        return code.strip().toUpperCase(Locale.ROOT);
    }

    private record PlayerRef(String code, String name) {
    }
}
