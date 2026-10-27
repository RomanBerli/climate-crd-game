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

        // 1. Check All-in-one CSV export (anonymized without phone number)
        String allCsv = adminService.exportSessionCsv(created.id(), "all");
        String[] allLines = allCsv.trim().split("\n");
        // 1 header + 60 investment rows (10 rounds * 6 players)
        assertThat(allLines).hasSize(61);
        assertThat(allLines[0]).contains("survey_expected_probability_pct,survey_reaction_unfairness,survey_motivation_ranking");
        assertThat(allCsv).doesNotContain("079 999 88 77");
        assertThat(allCsv).contains("Frau");

        // 2. Check RoundInfo CSV (6 player rows)
        String roundInfoCsv = adminService.exportSessionCsv(created.id(), "round_info");
        String[] roundInfoLines = roundInfoCsv.trim().split("\n");
        assertThat(roundInfoLines).hasSize(7);
        assertThat(roundInfoLines[0]).contains("session_code,game_mode,target_sum_rappen,total_invested_rappen,target_reached,risk_occurred,player_number,player_total_invested_rappen,balance_before_risk_rappen,final_payout_rappen,final_payout_chf,contract_join_choice,ever_joined_contract,left_contract,in_contract_at_end,left_contract_at_round");

        // 3. Check Invest CSV (60 investment rows)
        String investCsv = adminService.exportSessionCsv(created.id(), "invest");
        String[] investLines = investCsv.trim().split("\n");
        assertThat(investLines).hasSize(61);
        assertThat(investLines[0]).contains("session_code,round,player_number,invested_rappen,invested_chf,submitted_at,currently_in_contract");

        // 4. Check Survey CSV (6 player rows, without phone number)
        String surveyCsv = adminService.exportSessionCsv(created.id(), "survey");
        String[] surveyLines = surveyCsv.trim().split("\n");
        assertThat(surveyLines).hasSize(7);
        assertThat(surveyCsv).doesNotContain("079 999 88 77");
        assertThat(surveyCsv).contains("Frau");

        // 5. Check Auszahlung / Payout CSV (6 player rows, WITH phone number & CHF)
        String payoutCsv = adminService.exportSessionCsv(created.id(), "payout");
        String[] payoutLines = payoutCsv.trim().split("\n");
        assertThat(payoutLines).hasSize(7);
        assertThat(payoutLines[0]).contains("session_code,player_number,final_payout_rappen,final_payout_chf,survey_twint_number");
        assertThat(payoutCsv).contains("079 999 88 77");
        assertThat(payoutCsv).contains("2.00");
    }

    @Test
    void variantTwoWithContractsSupportsConditionalJoinAndExitFromRoundTwoBeforeAndAfterInvesting() {
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

        // Players 1..2 choose YES, Players 3..4 choose CONDITIONAL ("Nur wenn min. die Hälfte dabei"), Players 5..6 choose NO
        // Total potential members = 4 (>= 3) -> Players 1..4 all become members!
        String[] choices = {"YES", "YES", "CONDITIONAL", "CONDITIONAL", "NO", "NO"};
        for (int i = 0; i < 6; i++) {
            GameStateResponse afterJoinDecision = gameEngineService.submitContractJoin(players.get(i).sessionToken(), choices[i]);
            if (i < 5) {
                assertThat(afterJoinDecision.roundPhase()).isEqualTo(RoundPhase.CONTRACT_JOIN);
            } else {
                assertThat(afterJoinDecision.roundPhase()).isEqualTo(RoundPhase.INVESTMENT);
                assertThat(afterJoinDecision.contractState().contractActive()).isTrue();
                assertThat(afterJoinDecision.contractState().memberCount()).isEqualTo(4);
            }
        }

        // Verify Player 3 (CONDITIONAL) became a member
        GameStateResponse p3StateR1 = gameEngineService.getPlayerGameState(players.get(2).sessionToken());
        assertThat(p3StateR1.player().inContract()).isTrue();
        assertThat(p3StateR1.player().contractJoinChoice()).isEqualTo("CONDITIONAL");
        assertThat(p3StateR1.contractState().canLeaveContractNow()).isFalse();

        // Round 1: Leaving contract is NOT allowed in Round 1 ( neither before nor after investing )
        assertThatThrownBy(() -> gameEngineService.leaveContract(players.get(0).sessionToken()))
                .isInstanceOf(ResponseStatusException.class);

        // Round 1: Player 1 (contract member) cannot invest 0 Rappen (5% = 20 Rappen minimum enforced)
        assertThatThrownBy(() -> gameEngineService.submitInvestment(players.get(0).sessionToken(), 0))
                .isInstanceOf(ResponseStatusException.class);

        // Player 5 & 6 (not in contract) CAN invest 0 Rappen
        gameEngineService.submitInvestment(players.get(4).sessionToken(), 0);
        gameEngineService.submitInvestment(players.get(5).sessionToken(), 0);

        // Contract members 1..4 invest 20 Rappen each
        for (int i = 0; i < 4; i++) {
            gameEngineService.submitInvestment(players.get(i).sessionToken(), 20);
        }

        // Still in Round 1 after investing -> leaving still not allowed until Round 2
        assertThatThrownBy(() -> gameEngineService.leaveContract(players.get(0).sessionToken()))
                .isInstanceOf(ResponseStatusException.class);

        // Admin advances to Round 2 -> goes directly to INVESTMENT (no intermediate phase)
        AdminSessionResponse round2Admin = adminService.advanceRound(created.id());
        assertThat(round2Admin.currentRound()).isEqualTo(2);
        assertThat(round2Admin.roundPhase()).isEqualTo(RoundPhase.INVESTMENT);

        // In Round 2 BEFORE investing: Player 1 leaves the contract for free and can immediately invest 0 Rappen
        GameStateResponse p1AfterExit = gameEngineService.leaveContract(players.get(0).sessionToken());
        assertThat(p1AfterExit.player().inContract()).isFalse();
        assertThat(p1AfterExit.player().leftContract()).isTrue();
        assertThat(p1AfterExit.player().currentBalanceRappen()).isEqualTo(380);
        assertThat(p1AfterExit.contractState().memberCount()).isEqualTo(3);

        gameEngineService.submitInvestment(players.get(0).sessionToken(), 0);

        // Players 2, 3, 4 invest 20 Rappen in Round 2
        for (int i = 1; i < 4; i++) {
            gameEngineService.submitInvestment(players.get(i).sessionToken(), 20);
        }
        gameEngineService.submitInvestment(players.get(4).sessionToken(), 0);
        gameEngineService.submitInvestment(players.get(5).sessionToken(), 0);

        // In Round 2 AFTER investing: Player 2 also decides to leave the contract for future rounds
        GameStateResponse p2AfterExit = gameEngineService.leaveContract(players.get(1).sessionToken());
        assertThat(p2AfterExit.player().inContract()).isFalse();
        assertThat(p2AfterExit.player().leftContract()).isTrue();
        assertThat(p2AfterExit.contractState().memberCount()).isEqualTo(2);

        // Verify RoundInfo CSV tracks contract_join_choice and left_contract_at_round
        String roundInfoCsv = adminService.exportSessionCsv(created.id(), "round_info");
        String[] roundInfoLines = roundInfoCsv.trim().split("\n");
        assertThat(roundInfoLines[1]).contains(",1,20,380,380,3.80,YES,true,true,false,2");
        assertThat(roundInfoLines[3]).contains(",3,40,360,360,3.60,CONDITIONAL,true,false,true,");
        assertThat(roundInfoLines[5]).contains(",5,0,400,400,4.00,NO,false,false,false,");
    }

    @Test
    void conditionalJoinDoesNotBecomeMemberWhenFewerThanHalfWantToJoin() {
        AdminSessionResponse created = adminService.createSession(GameMode.WITH_CONTRACTS);
        List<JoinLobbyResponse> players = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            players.add(gameEngineService.joinLobby(created.code(), null));
        }
        adminService.startSession(created.id());

        // Player 1 chooses YES, Player 2 chooses CONDITIONAL, Players 3..6 choose NO
        // Potential members = 2 (< 3) -> Only Player 1 becomes a member, Player 2 becomes a non-member!
        String[] choices = {"YES", "CONDITIONAL", "NO", "NO", "NO", "NO"};
        for (int i = 0; i < 6; i++) {
            gameEngineService.submitContractJoin(players.get(i).sessionToken(), choices[i]);
        }

        GameStateResponse p1State = gameEngineService.getPlayerGameState(players.get(0).sessionToken());
        GameStateResponse p2State = gameEngineService.getPlayerGameState(players.get(1).sessionToken());

        assertThat(p1State.player().inContract()).isTrue();
        assertThat(p2State.player().inContract()).isFalse();
        assertThat(p2State.player().everJoinedContract()).isFalse();
        assertThat(p1State.contractState().memberCount()).isEqualTo(1);
    }
}
