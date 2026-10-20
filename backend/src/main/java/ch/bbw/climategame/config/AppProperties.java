package ch.bbw.climategame.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String adminSecret;
    private GameConfig game = new GameConfig();

    public String getAdminSecret() {
        return adminSecret;
    }

    public void setAdminSecret(String adminSecret) {
        this.adminSecret = adminSecret;
    }

    public GameConfig getGame() {
        return game;
    }

    public void setGame(GameConfig game) {
        this.game = game;
    }

    public static class GameConfig {
        private double startAmountChf = 4.0;
        private int groupSize = 6;
        private int totalRounds = 10;
        private int regularPhaseSeconds = 60;
        private int overtimePhaseSeconds = 30;

        public double getStartAmountChf() {
            return startAmountChf;
        }

        public void setStartAmountChf(double startAmountChf) {
            this.startAmountChf = startAmountChf;
        }

        public int getGroupSize() {
            return groupSize;
        }

        public void setGroupSize(int groupSize) {
            this.groupSize = groupSize;
        }

        public int getTotalRounds() {
            return totalRounds;
        }

        public void setTotalRounds(int totalRounds) {
            this.totalRounds = totalRounds;
        }

        public int getRegularPhaseSeconds() {
            return regularPhaseSeconds;
        }

        public void setRegularPhaseSeconds(int regularPhaseSeconds) {
            this.regularPhaseSeconds = regularPhaseSeconds;
        }

        public int getOvertimePhaseSeconds() {
            return overtimePhaseSeconds;
        }

        public void setOvertimePhaseSeconds(int overtimePhaseSeconds) {
            this.overtimePhaseSeconds = overtimePhaseSeconds;
        }

        public int getStartAmountRappen() {
            return (int) Math.round(startAmountChf * 100.0);
        }

        public int getTargetSumRappen() {
            return (getStartAmountRappen() / 2) * groupSize;
        }

        public int getInvestOption0Rappen() {
            return 0;
        }

        public int getInvestOption1Rappen() {
            return (int) Math.round(getStartAmountRappen() * 0.05);
        }

        public int getInvestOption2Rappen() {
            return (int) Math.round(getStartAmountRappen() * 0.10);
        }

        public List<Integer> getAllowedInvestmentOptions() {
            return List.of(getInvestOption0Rappen(), getInvestOption1Rappen(), getInvestOption2Rappen());
        }
    }
}
