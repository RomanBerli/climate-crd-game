package ch.bbw.climategame.dto.response;

import ch.bbw.climategame.entity.RoundPhase;

public record ContractStateDto(
        RoundPhase roundPhase,
        boolean contractActive,
        boolean contractAbolished,
        int memberCount,
        int minInvestmentRappen,
        int exitFeePerMemberRappen,
        int totalExitFeeForPlayerRappen,
        boolean canAffordExit,
        boolean voteAlreadyHeldThisRound,
        Integer voteInitiatorPlayerNumber,
        boolean hasDecidedCurrentContractPhase,
        String lastContractEvent
) {
}
