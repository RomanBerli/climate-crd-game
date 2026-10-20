package ch.bbw.climategame.dto.response;

import java.math.BigDecimal;

public record EndgameResultDto(
        boolean isGoalReached,
        Boolean riskOccurred,
        BigDecimal riskLevel,
        int riskPercent,
        int finalPayoutRappen,
        int remainingBalanceBeforeRiskRappen,
        String scenarioCode,
        String message
) {
}
