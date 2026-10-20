package ch.bbw.climategame.dto.response;

import ch.bbw.climategame.entity.SessionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminSessionResponse(
        UUID id,
        String code,
        SessionStatus status,
        BigDecimal riskLevel,
        int riskPercent,
        int currentRound,
        int totalRounds,
        boolean isPaused,
        int targetSumRappen,
        int totalPotRappen,
        int connectedPlayersCount,
        int readyPlayersCount,
        int groupSize,
        boolean canAdvanceRound,
        Boolean isGoalReached,
        Boolean riskOccurred,
        Instant createdAt,
        Instant updatedAt,
        List<AdminPlayerDto> players
) {
    public record AdminPlayerDto(
            int playerNumber,
            boolean isConnected,
            boolean isReadyCurrentRound,
            Integer finalPayoutRappen,
            Instant lastSeenAt
    ) {
    }
}
