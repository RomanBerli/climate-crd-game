package ch.bbw.climategame;

import ch.bbw.climategame.dto.request.SubmitSurveyRequest;
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
    void fullMilinskiExperimentWithSurveyAndCsvExport() {
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

            // Admin clicks next round (or moves to SURVEY after round 10)
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
                // After Round 10 -> Session is FINISHED, players individually fill survey before seeing result
                assertThat(advanced.status()).isEqualTo(SessionStatus.FINISHED);
                assertThat(advanced.readyPlayersCount()).isEqualTo(0);
            }
        }

        // Before submitting the survey, the player's state is FINISHED with hasSubmittedSurvey=false and endgame=null
        GameStateResponse stateBeforeSurvey = gameEngineService.getPlayerGameState(joinedPlayers.get(0).sessionToken());
        assertThat(stateBeforeSurvey.status()).isEqualTo(SessionStatus.FINISHED);
        assertThat(stateBeforeSurvey.player().hasSubmittedSurvey()).isFalse();
        assertThat(stateBeforeSurvey.endgame()).isNull();

        // Players 1 to 6 submit their survey and immediately receive the final result in the response
        for (int i = 0; i < 6; i++) {
            GameStateResponse afterSurvey = gameEngineService.submitSurvey(
                    joinedPlayers.get(i).sessionToken(),
                    new SubmitSurveyRequest(
                            75,
                            "Ich habe gleich weitergemacht.",
                            List.of(
                                    "dass die Gruppe das Ziel erreicht",
                                    "fair zu sein bzw. meinen Anteil zu leisten",
                                    "den Verlust des eigenen Geldes vermeiden",
                                    "möglichst viel eigenes Geld behalten",
                                    "den Preis zu gewinnen"
                            ),
                            null,
                            null,
                            null,
                            null,
                            18 + i,
                            i == 5 ? "Frau" : "Mann",
                            i == 5 ? "079 999 88 77" : "079 111 22 3" + i
                    )
            );
            assertThat(afterSurvey.status()).isEqualTo(SessionStatus.FINISHED);
            assertThat(afterSurvey.player().hasSubmittedSurvey()).isTrue();
            assertThat(afterSurvey.endgame()).isNotNull();
            assertThat(afterSurvey.endgame().isGoalReached()).isTrue();
            assertThat(afterSurvey.endgame().finalPayoutRappen()).isEqualTo(200);
            assertThat(afterSurvey.endgame().message()).isEqualTo("Klimaziel erreicht! Ihr Restguthaben wird ausbezahlt.");
        }

        AdminSessionResponse surveyReadyState = adminService.getSession(created.id());
        assertThat(surveyReadyState.readyPlayersCount()).isEqualTo(6);

        // Check CSV export includes survey columns and answers
        String csv = adminService.exportSessionCsv(created.id());
        String[] lines = csv.trim().split("\n");
        // 1 header + 60 investment rows (10 rounds * 6 players)
        assertThat(lines).hasSize(61);
        assertThat(lines[0]).contains("survey_expected_probability_pct,survey_reaction_unfairness,survey_motivation_ranking");
        assertThat(csv).contains("079 999 88 77");
        assertThat(csv).contains("Frau");
    }
}
