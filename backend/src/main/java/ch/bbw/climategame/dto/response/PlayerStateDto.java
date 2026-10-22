package ch.bbw.climategame.dto.response;

import java.util.List;

public record PlayerStateDto(
        int playerNumber,
        int initialBalanceRappen,
        int currentBalanceRappen,
        int ownTotalInvestedRappen,
        boolean hasSubmittedCurrentRound,
        boolean hasSubmittedSurvey,
        boolean inContract,
        boolean everJoinedContract,
        boolean leftContract,
        Integer currentRoundInvestmentRappen,
        List<Integer> allowedInvestmentOptions,
        Integer finalPayoutRappen
) {
}
