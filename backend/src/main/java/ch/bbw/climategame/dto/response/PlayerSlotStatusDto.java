package ch.bbw.climategame.dto.response;

public record PlayerSlotStatusDto(
        int playerNumber,
        boolean isConnected,
        boolean isReady,
        boolean inContract,
        boolean leftContract
) {
}
