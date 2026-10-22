package ch.bbw.climategame;

import ch.bbw.climategame.dto.request.SubmitSurveyRequest;
import ch.bbw.climategame.dto.response.AdminSessionResponse;
import ch.bbw.climategame.dto.response.GameStateResponse;
import ch.bbw.climategame.dto.response.JoinLobbyResponse;
import ch.bbw.climategame.entity.GameMode;
import ch.bbw.climategame.entity.Player;
import ch.bbw.climategame.entity.RoundPhase;
import ch.bbw.climategame.entity.SessionStatus;
import ch.bbw.climategame.repository.PlayerRepository;
import ch.bbw.climategame.service.AdminService;
import ch.bbw.climategame.service.GameEngineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
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
    private PlayerRepository playerRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void lobbyLeaveRemovesPlayerImmediatelyAndInactivePlayerIsKickedAfter30s() {
        AdminSessionResponse session = adminService.createSession(GameMode.NORMAL);

        // Player 1 joins and leaves via UI
        JoinLobbyResponse p1 = gameEngineService.joinLobby(session.code(), null);
        assertThat(p1.playerNumber()).isEqualTo(1);
        assertThat(adminService.getSession(session.id()).connectedPlayersCount()).isEqualTo(1);

        gameEngineService.leaveLobby(p1.sessionToken());
        assertThat(adminService.getSession(session.id()).connectedPlayersCount()).isEqualTo(0);
        assertThatThrownBy(() -> gameEngineService.getPlayerGameState(p1.sessionToken()))
                .isInstanceOf(ResponseStatusException.class);

        // Next player joining gets slot 1 again
        JoinLobbyResponse p1New = gameEngineService.joinLobby(session.code(), null);
        assertThat(p1New.playerNumber()).isEqualTo(1);

        // Simulate Player 1 being inactive (not polling) for > 30 seconds
        Player entity = playerRepository.findBySessionToken(p1New.sessionToken()).orElseThrow();
        entity.setLastSeenAt(Instant.now().minusSeconds(35));
        playerRepository.save(entity);

        // Next check kicks inactive player automatically
        assertThat(adminService.getSession(session.id()).connectedPlayersCount()).isEqualTo(0);
        assertThatThrownBy(() -> gameEngineService.getPlayerGameState(p1New.sessionToken()))
                .isInstanceOf(ResponseStatusException.class);
    }

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

                // Verify player only sees the single last completed round in history and NOT the cumulative total pot
                GameStateResponse stateInNextRound = gameEngineService.getPlayerGameState(joinedPlayers.get(0).sessionToken());
                assertThat(stateInNextRound.history()).hasSize(1);
                assertThat(stateInNextRound.history().get(0).round()).isEqualTo(round);
                assertThat(stateInNextRound.history().get(0).roundTotalRappen()).isEqualTo(120);
                assertThat(stateInNextRound.totalPotRappen()).isEqualTo(0);
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

    @Test
    void variantTwoWithContractsEnforcesMinimumAndSupportsPaidExitAndMajorityAbolition() {
        AdminSessionResponse created = adminService.createSession(GameMode.WITH_CONTRACTS);
        assertThat(created.gameMode()).isEqualTo(GameMode.WITH_CONTRACTS);

        List<JoinLobbyResponse> players = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            players.add(gameEngineService.joinLobby(created.code(), null));
        }

        // Start session -> enters CONTRACT_JOIN before Round 1
        AdminSessionResponse started = adminService.startSession(created.id());
        assertThat(started.status()).isEqualTo(SessionStatus.RUNNING);
        assertThat(started.currentRound()).isEqualTo(1);
        assertThat(started.roundPhase()).isEqualTo(RoundPhase.CONTRACT_JOIN);

        // Players 1..4 join the contract, Players 5..6 do not join
        for (int i = 0; i < 6; i++) {
            boolean join = i < 4;
            GameStateResponse afterJoinDecision = gameEngineService.submitContractJoin(players.get(i).sessionToken(), join);
            if (i < 5) {
                assertThat(afterJoinDecision.roundPhase()).isEqualTo(RoundPhase.CONTRACT_JOIN);
            } else {
                // Once all 6 decided -> automatically switches to Round 1 INVESTMENT
                assertThat(afterJoinDecision.roundPhase()).isEqualTo(RoundPhase.INVESTMENT);
                assertThat(afterJoinDecision.contractState().contractActive()).isTrue();
                assertThat(afterJoinDecision.contractState().memberCount()).isEqualTo(4);
            }
        }

        // Round 1: Player 1 (contract member) cannot invest 0 Rappen (5% = 20 Rappen minimum enforced)
        assertThatThrownBy(() -> gameEngineService.submitInvestment(players.get(0).sessionToken(), 0))
                .isInstanceOf(ResponseStatusException.class);

        // Player 5 (not in contract) CAN invest 0 Rappen
        gameEngineService.submitInvestment(players.get(4).sessionToken(), 0);
        gameEngineService.submitInvestment(players.get(5).sessionToken(), 0);

        // Contract members 1..4 invest 20 Rappen each (balance becomes 400 - 20 = 380)
        for (int i = 0; i < 4; i++) {
            gameEngineService.submitInvestment(players.get(i).sessionToken(), 20);
        }

        // Admin advances to Round 2 -> enters CONTRACT_REVIEW between Round 1 and Round 2
        AdminSessionResponse round2Admin = adminService.advanceRound(created.id());
        assertThat(round2Admin.currentRound()).isEqualTo(2);
        assertThat(round2Admin.roundPhase()).isEqualTo(RoundPhase.CONTRACT_REVIEW);

        // Player 1 exits contract in between rounds: pays 20 Rappen to each of the 3 remaining members (Players 2, 3, 4)
        // Player 1 balance: 380 - (3 * 20) = 320 Rappen
        // Players 2, 3, 4 balance: 380 + 20 = 400 Rappen
        GameStateResponse p1AfterExit = gameEngineService.submitContractReviewAction(players.get(0).sessionToken(), "LEAVE");
        assertThat(p1AfterExit.player().inContract()).isFalse();
        assertThat(p1AfterExit.player().leftContract()).isTrue();
        assertThat(p1AfterExit.player().currentBalanceRappen()).isEqualTo(320);
        assertThat(p1AfterExit.contractState().memberCount()).isEqualTo(3);

        // Remaining members 2, 3, 4 choose STAY -> transitions to Round 2 INVESTMENT
        gameEngineService.submitContractReviewAction(players.get(1).sessionToken(), "STAY");
        gameEngineService.submitContractReviewAction(players.get(2).sessionToken(), "STAY");
        GameStateResponse p4AfterStay = gameEngineService.submitContractReviewAction(players.get(3).sessionToken(), "STAY");
        assertThat(p4AfterStay.roundPhase()).isEqualTo(RoundPhase.INVESTMENT);
        assertThat(p4AfterStay.player().currentBalanceRappen()).isEqualTo(400);

        // In Round 2, Player 1 (who left contract) can now invest 0 Rappen!
        gameEngineService.submitInvestment(players.get(0).sessionToken(), 0);
        for (int i = 1; i < 4; i++) {
            gameEngineService.submitInvestment(players.get(i).sessionToken(), 20);
        }
        gameEngineService.submitInvestment(players.get(4).sessionToken(), 0);
        gameEngineService.submitInvestment(players.get(5).sessionToken(), 0);

        // Admin advances to Round 3 -> enters CONTRACT_REVIEW for remaining 3 members (Players 2, 3, 4)
        AdminSessionResponse round3Admin = adminService.advanceRound(created.id());
        assertThat(round3Admin.currentRound()).isEqualTo(3);
        assertThat(round3Admin.roundPhase()).isEqualTo(RoundPhase.CONTRACT_REVIEW);

        // Player 2 initiates vote to abolish contract -> switches to CONTRACT_VOTE (Player 2 auto-votes YES)
        GameStateResponse afterStartVote = gameEngineService.submitContractReviewAction(players.get(1).sessionToken(), "START_VOTE");
        assertThat(afterStartVote.roundPhase()).isEqualTo(RoundPhase.CONTRACT_VOTE);

        // Player 3 votes YES (for abolition), Player 4 votes NO -> 2 of 3 (> 50%) majority abolishes contract for free!
        gameEngineService.submitContractVote(players.get(2).sessionToken(), true);
        GameStateResponse afterVoteResolved = gameEngineService.submitContractVote(players.get(3).sessionToken(), false);
        assertThat(afterVoteResolved.roundPhase()).isEqualTo(RoundPhase.INVESTMENT);
        assertThat(afterVoteResolved.contractState().contractActive()).isFalse();
        assertThat(afterVoteResolved.contractState().contractAbolished()).isTrue();
        assertThat(afterVoteResolved.contractState().memberCount()).isEqualTo(0);

        // Now former members can invest 0 Rappen without paying any exit fee
        gameEngineService.submitInvestment(players.get(1).sessionToken(), 0);
    }
}
