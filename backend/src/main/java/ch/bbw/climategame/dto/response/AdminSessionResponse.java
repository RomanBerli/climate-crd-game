package ch.bbw.climategame.dto.response;

import ch.bbw.climategame.entity.RoundPhase;
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
        RoundPhase roundPhase,
        int remainingSeconds,
        boolean isPaused,
        int targetSumRappen,
        int totalPotRappen,
        int connectedPlayersCount,
        int groupSize,
        Boolean isGoalReached,
        Boolean riskOccurred,
        Instant createdAt,
        Instant updatedAt,
        List<AdminPlayerDto> players,
        List<RoundHistoryDto> history
) {
    public record AdminPlayerDto(
            int playerNumber,
            boolean isConnected,
            boolean isReadyCurrentRound,
            int initialBalanceRappen,
            int currentBalanceRappen,
            int totalInvestedRappen,
            Integer currentRoundInvestmentRappen,
            Integer finalPayoutRappen,
            Instant lastSeenAt
    ) {
    }
}
