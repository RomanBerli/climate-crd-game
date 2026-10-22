package ch.bbw.climategame.dto.response;

import java.util.List;

public record PlayerStateDto(
        int playerNumber,
        int initialBalanceRappen,
        int currentBalanceRappen,
        int ownTotalInvestedRappen,
        boolean hasSubmittedCurrentRound,
        boolean hasSubmittedSurvey,
        Integer currentRoundInvestmentRappen,
        List<Integer> allowedInvestmentOptions,
        Integer finalPayoutRappen
) {
}
