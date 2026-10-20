package ch.bbw.climategame.dto.response;

import java.util.List;

public record PlayerStateDto(
        int playerNumber,
        int initialBalanceRappen,
        int currentBalanceRappen,
        int ownTotalInvestedRappen,
        boolean hasSubmittedCurrentRound,
        Integer currentRoundInvestmentRappen,
        List<Integer> allowedInvestmentOptions,
        Integer finalPayoutRappen
) {
}
