package ch.bbw.climategame.dto.response;

import ch.bbw.climategame.entity.GameMode;
import ch.bbw.climategame.entity.SessionStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record GameStateResponse(
        UUID sessionId,
        String sessionCode,
        SessionStatus status,
        GameMode gameMode,
        BigDecimal riskLevel,
        int riskPercent,
        int targetSumRappen,
        int totalPotRappen,
        int currentRound,
        int totalRounds,
        int connectedPlayersCount,
        int groupSize,
        PlayerStateDto player,
        List<PlayerSlotStatusDto> playersStatus,
        List<RoundHistoryDto> history,
        EndgameResultDto endgame
) {
}
