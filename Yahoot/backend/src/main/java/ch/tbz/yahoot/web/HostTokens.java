package ch.tbz.yahoot.web;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Nur wer das Spiel erstellt hat, kennt das Token und darf es steuern. */
@Component
class HostTokens {

    private final SecureRandom random = new SecureRandom();
    private final Map<String, String> tokens = new ConcurrentHashMap<>();

    String issue(String code) {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        tokens.put(key(code), token);
        return token;
    }

    void check(String code, String token) {
        String expected = tokens.get(key(code));
        if (expected == null || token == null
                || !java.security.MessageDigest.isEqual(expected.getBytes(), token.getBytes())) {
            throw new NotHostException();
        }
    }

    private static String key(String code) {
        return code.strip().toUpperCase(Locale.ROOT);
    }

    static final class NotHostException extends RuntimeException {
        NotHostException() {
            super("Nur der Host darf das Spiel steuern");
        }
    }
}
