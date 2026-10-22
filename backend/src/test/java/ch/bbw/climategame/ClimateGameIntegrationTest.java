package ch.bbw.climategame;

import ch.bbw.climategame.dto.response.AdminSessionResponse;
import ch.bbw.climategame.dto.response.GameStateResponse;
import ch.bbw.climategame.dto.response.JoinLobbyResponse;
import ch.bbw.climategame.entity.GameMode;
import ch.bbw.climategame.entity.SessionStatus;
import ch.bbw.climategame.service.AdminService;
import ch.bbw.climategame.service.GameEngineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClimateGameIntegrationTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private GameEngineService gameEngineService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminEndpointsRequireValidSpringSecurityAuthentication() throws Exception {
        // Without header -> 401 Unauthorized
        mockMvc.perform(get("/api/admin/sessions"))
                .andExpect(status().isUnauthorized());

        // With wrong header -> 401 Unauthorized
        mockMvc.perform(get("/api/admin/sessions").header("X-Admin-Key", "wrong_secret"))
                .andExpect(status().isUnauthorized());

        // With valid env secret ("test_admin_secret") -> 200 OK
        mockMvc.perform(get("/api/admin/sessions").header("X-Admin-Key", "test_admin_secret"))
                .andExpect(status().isOk());
    }

    @Test
    void fullMilinskiExperimentWithManualAdminRoundAdvanceAndCsvExport() {
        AdminSessionResponse created = adminService.createSession(GameMode.NORMAL);
        assertThat(created.code()).hasSize(4);
        assertThat(created.gameMode()).isEqualTo(GameMode.NORMAL);
        assertThat(created.riskPercent()).isEqualTo(90);
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
        assertThat(started.canAdvanceRound()).isFalse();

        // Play all 10 rounds: Admin advances round only after all 6 players have chosen
        for (int round = 1; round <= 10; round++) {
            for (int i = 0; i < 5; i++) {
                gameEngineService.submitInvestment(joinedPlayers.get(i).sessionToken(), 20);
            }

            // Only 5/6 players have chosen -> Admin cannot advance round yet
            AdminSessionResponse partialAdminState = adminService.getSession(created.id());
            assertThat(partialAdminState.readyPlayersCount()).isEqualTo(5);
            assertThat(partialAdminState.canAdvanceRound()).isFalse();
            assertThatThrownBy(() -> adminService.advanceRound(created.id()))
                    .isInstanceOf(ResponseStatusException.class);

            // 6th player submits
            gameEngineService.submitInvestment(joinedPlayers.get(5).sessionToken(), 20);

            // Round does not advance automatically until Admin clicks "Nächste Runde"
            GameStateResponse stateBeforeAdminClick = gameEngineService.getPlayerGameState(joinedPlayers.get(0).sessionToken());
            assertThat(stateBeforeAdminClick.currentRound()).isEqualTo(round);
            assertThat(stateBeforeAdminClick.status()).isEqualTo(SessionStatus.RUNNING);

            AdminSessionResponse readyAdminState = adminService.getSession(created.id());
            assertThat(readyAdminState.readyPlayersCount()).isEqualTo(6);
            assertThat(readyAdminState.canAdvanceRound()).isTrue();

            // Admin clicks next round (or finishes after round 10)
            AdminSessionResponse advanced = adminService.advanceRound(created.id());
            if (round < 10) {
                assertThat(advanced.currentRound()).isEqualTo(round + 1);
                assertThat(advanced.status()).isEqualTo(SessionStatus.RUNNING);

                // Verify player only sees the single last completed round in history + total pot after round
                GameStateResponse stateInNextRound = gameEngineService.getPlayerGameState(joinedPlayers.get(0).sessionToken());
                assertThat(stateInNextRound.history()).hasSize(1);
                assertThat(stateInNextRound.history().get(0).round()).isEqualTo(round);
                assertThat(stateInNextRound.history().get(0).roundTotalRappen()).isEqualTo(120);
                assertThat(stateInNextRound.history().get(0).cumulativePotRappen()).isEqualTo(round * 120);
                assertThat(stateInNextRound.totalPotRappen()).isEqualTo(round * 120);
            } else {
                assertThat(advanced.status()).isEqualTo(SessionStatus.FINISHED);
            }
        }

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
