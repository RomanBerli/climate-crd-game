package ch.bbw.climategame.dto.response;

import ch.bbw.climategame.entity.GameMode;
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
        GameMode gameMode,
        RoundPhase roundPhase,
        boolean contractActive,
        int contractMemberCount,
        BigDecimal riskLevel,
        int riskPercent,
        int currentRound,
        int totalRounds,
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
            boolean inContract,
            boolean leftContract,
            Integer finalPayoutRappen,
            Instant lastSeenAt
    ) {
    }
}
