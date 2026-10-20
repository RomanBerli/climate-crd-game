package ch.bbw.climategame.dto.response;

public record PlayerSlotStatusDto(
        int playerNumber,
        boolean isConnected,
        boolean isReady
) {
}
