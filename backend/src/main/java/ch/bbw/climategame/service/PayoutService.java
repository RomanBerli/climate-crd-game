package ch.bbw.climategame.service;

import ch.bbw.climategame.dto.response.EndgameResultDto;
import ch.bbw.climategame.entity.GameSession;
import ch.bbw.climategame.entity.Player;
import ch.bbw.climategame.entity.RoundInvestment;
import ch.bbw.climategame.entity.SessionStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;

@Service
public class PayoutService {

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Führt nach Runde 10 die Milinski-Endabrechnung durch (Spec 1.3 & 6, Plan 2.5).
     */
    public void resolveEndgame(GameSession session, List<Player> players, List<RoundInvestment> allInvestments) {
        int totalInvestments = allInvestments.stream()
                .mapToInt(RoundInvestment::getAmountRappen)
                .sum();

        session.setTotalPotRappen(totalInvestments);
        boolean goalReached = totalInvestments >= session.getTargetSumRappen();
        session.setIsGoalReached(goalReached);

        boolean riskOccurred = false;
        if (!goalReached) {
            double roll = secureRandom.nextDouble();
            riskOccurred = roll < session.getRiskLevel().doubleValue();
        }
        session.setRiskOccurred(riskOccurred);

        for (Player player : players) {
            int ownInvestments = allInvestments.stream()
                    .filter(inv -> inv.getPlayerNumber() == player.getPlayerNumber())
                    .mapToInt(RoundInvestment::getAmountRappen)
                    .sum();
            int remainingBalance = Math.max(0, player.getInitialBalanceRappen() - ownInvestments);
            player.setCurrentBalanceRappen(remainingBalance);

            if (goalReached || !riskOccurred) {
                player.setFinalPayoutRappen(remainingBalance);
            } else {
                player.setFinalPayoutRappen(0);
            }
        }

        session.setStatus(SessionStatus.FINISHED);
    }

    public EndgameResultDto buildEndgameResultDto(GameSession session, Player player) {
        if (session.getStatus() != SessionStatus.FINISHED) {
            return null;
        }
        boolean goalReached = Boolean.TRUE.equals(session.getIsGoalReached());
        boolean riskOccurred = Boolean.TRUE.equals(session.getRiskOccurred());
        BigDecimal riskLevel = session.getRiskLevel();
        int riskPercent = (int) Math.round(riskLevel.doubleValue() * 100.0);
        int remainingBeforeRisk = player.getCurrentBalanceRappen();
        int finalPayout = player.getFinalPayoutRappen() != null ? player.getFinalPayoutRappen() : remainingBeforeRisk;

        String scenarioCode;
        String message;
        if (goalReached) {
            scenarioCode = "SCENARIO_A_GOAL_REACHED";
            message = "Klimaziel erreicht! Ihr Restguthaben wird ausbezahlt.";
        } else if (!riskOccurred) {
            scenarioCode = "SCENARIO_B_GOAL_MISSED_LUCK";
            message = "Ziel verfehlt. Sie hatten Glück: Ihr Restguthaben bleibt erhalten.";
        } else {
            scenarioCode = "SCENARIO_C_GOAL_MISSED_LOSS";
            message = "Ziel verfehlt. Ihr Geld ging durch das " + riskPercent + "% Risiko verloren. Auszahlung: 0 Fr.";
        }

        return new EndgameResultDto(
                goalReached,
                riskOccurred,
                riskLevel,
                riskPercent,
                finalPayout,
                remainingBeforeRisk,
                scenarioCode,
                message
        );
    }
}
