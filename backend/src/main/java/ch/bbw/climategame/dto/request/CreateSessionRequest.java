package ch.bbw.climategame.dto.request;

import ch.bbw.climategame.entity.GameMode;

public record CreateSessionRequest(
        GameMode gameMode
) {
}
