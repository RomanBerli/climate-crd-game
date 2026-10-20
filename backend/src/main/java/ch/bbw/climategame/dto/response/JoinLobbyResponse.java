package ch.bbw.climategame.dto.response;

import java.util.UUID;

public record JoinLobbyResponse(
        UUID sessionId,
        String sessionCode,
        int playerNumber,
        String sessionToken
) {
}
