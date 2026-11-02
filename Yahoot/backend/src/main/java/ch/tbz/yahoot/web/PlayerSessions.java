package ch.tbz.yahoot.web;

import ch.tbz.yahoot.game.GameRuleException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Merkt sich, welcher Spieler hinter einer WebSocket-Sitzung steckt. So kann niemand im Namen eines
 * anderen antworten oder Karten spielen.
 */
@Component
class PlayerSessions {

    private final Map<String, PlayerRef> bySession = new ConcurrentHashMap<>();

    void register(String sessionId, String code, String name) {
        bySession.put(sessionId, new PlayerRef(normalize(code), name));
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
