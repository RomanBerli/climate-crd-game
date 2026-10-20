package ch.bbw.climategame;

import ch.bbw.climategame.dto.response.AdminSessionResponse;
import ch.bbw.climategame.dto.response.GameStateResponse;
import ch.bbw.climategame.dto.response.JoinLobbyResponse;
import ch.bbw.climategame.entity.GameSession;
import ch.bbw.climategame.entity.RoundPhase;
import ch.bbw.climategame.entity.SessionStatus;
import ch.bbw.climategame.repository.GameSessionRepository;
import ch.bbw.climategame.service.AdminService;
import ch.bbw.climategame.service.GameEngineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ClimateGameIntegrationTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private GameEngineService gameEngineService;

    @Autowired
    private GameSessionRepository sessionRepository;

    @Test
    void fullMilinskiExperimentGoalReachedAndCsvExport() {
        AdminSessionResponse created = adminService.createSession(new BigDecimal("0.90"));
        assertThat(created.code()).hasSize(4);
        assertThat(created.targetSumRappen()).isEqualTo(1200);
        assertThat(created.status()).isEqualTo(SessionStatus.LOBBY);

        List<JoinLobbyResponse> joinedPlayers = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            JoinLobbyResponse join = gameEngineService.joinLobby(created.code(), null);
            assertThat(join.playerNumber()).isEqualTo(i);
            joinedPlayers.add(join);
        }

        // Start session
        AdminSessionResponse started = adminService.startSession(created.id());
        assertThat(started.status()).isEqualTo(SessionStatus.RUNNING);
        assertThat(started.currentRound()).isEqualTo(1);
        assertThat(started.roundPhase()).isEqualTo(RoundPhase.REGULAR);

        // Pause & Resume verification
        AdminSessionResponse paused = adminService.pauseSession(created.id());
        assertThat(paused.status()).isEqualTo(SessionStatus.PAUSED);
        assertThat(paused.isPaused()).isTrue();

        GameStateResponse pausedPlayerState = gameEngineService.getPlayerGameState(joinedPlayers.get(0).sessionToken());
        assertThat(pausedPlayerState.isPaused()).isTrue();

        AdminSessionResponse resumed = adminService.resumeSession(created.id());
        assertThat(resumed.status()).isEqualTo(SessionStatus.RUNNING);
        assertThat(resumed.isPaused()).isFalse();

        // Play rounds 1 to 9 manually (each player invests 20 Rappen -> 120 Rappen per round)
        for (int round = 1; round <= 9; round++) {
            for (JoinLobbyResponse p : joinedPlayers) {
                gameEngineService.submitInvestment(p.sessionToken(), 20);
            }
            GameStateResponse stateAfterRound = gameEngineService.getPlayerGameState(joinedPlayers.get(0).sessionToken());
            assertThat(stateAfterRound.currentRound()).isEqualTo(round + 1);
            assertThat(stateAfterRound.history()).hasSize(round);
        }

        // In Round 10: test Overtime + Auto-Bet for Player 6 while Players 1-5 invest 20 Rappen
        for (int i = 0; i < 5; i++) {
            gameEngineService.submitInvestment(joinedPlayers.get(i).sessionToken(), 20);
        }

        // Expire Regular deadline -> transitions to OVERTIME
        GameSession sessionEntity = sessionRepository.findById(created.id()).orElseThrow();
        sessionEntity.setRoundDeadline(Instant.now().minusSeconds(1));
        sessionRepository.save(sessionEntity);
        gameEngineService.processSessionTimerTick(created.id());

        GameStateResponse overtimeState = gameEngineService.getPlayerGameState(joinedPlayers.get(5).sessionToken());
        assertThat(overtimeState.roundPhase()).isEqualTo(RoundPhase.OVERTIME);

        // Expire Overtime deadline -> triggers Auto-Bet (20 Rappen) for Player 6 and resolves Round 10
        sessionEntity = sessionRepository.findById(created.id()).orElseThrow();
        sessionEntity.setRoundDeadline(Instant.now().minusSeconds(1));
        sessionRepository.save(sessionEntity);
        gameEngineService.processSessionTimerTick(created.id());

        // Check final state
        GameStateResponse finalState = gameEngineService.getPlayerGameState(joinedPlayers.get(5).sessionToken());
        assertThat(finalState.status()).isEqualTo(SessionStatus.FINISHED);
        assertThat(finalState.endgame()).isNotNull();
        assertThat(finalState.endgame().isGoalReached()).isTrue();
        assertThat(finalState.endgame().finalPayoutRappen()).isEqualTo(200);
        assertThat(finalState.endgame().message()).isEqualTo("Klimaziel erreicht! Ihr Restguthaben wird ausbezahlt.");

        // Check CSV export
        String csv = adminService.exportSessionCsv(created.id());
        String[] lines = csv.trim().split("\n");
        // 1 header + 60 investment rows (10 rounds * 6 players)
        assertThat(lines).hasSize(61);
        assertThat(lines[0]).startsWith("session_id,session_code,risk_treatment,target_sum_rappen,total_invested_rappen");
    }
}
