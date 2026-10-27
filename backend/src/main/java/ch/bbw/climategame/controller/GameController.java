package ch.bbw.climategame.controller;

import ch.bbw.climategame.dto.request.ContractJoinRequest;
import ch.bbw.climategame.dto.request.InvestRequest;
import ch.bbw.climategame.dto.request.SubmitSurveyRequest;
import ch.bbw.climategame.dto.response.GameStateResponse;
import ch.bbw.climategame.service.GameEngineService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameEngineService gameEngineService;

    public GameController(GameEngineService gameEngineService) {
        this.gameEngineService = gameEngineService;
    }

    @GetMapping("/state")
    public ResponseEntity<GameStateResponse> getGameState(
            HttpServletRequest request,
            @RequestHeader(value = "X-Player-Token", required = false) String xPlayerToken,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        String token = LobbyController.resolveToken(request, xPlayerToken, authHeader);
        GameStateResponse state = gameEngineService.getPlayerGameState(token);
        return ResponseEntity.ok(state);
    }

    @PostMapping("/invest")
    public ResponseEntity<GameStateResponse> submitInvestment(
            @Valid @RequestBody InvestRequest investRequest,
            HttpServletRequest request,
            @RequestHeader(value = "X-Player-Token", required = false) String xPlayerToken,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        String token = LobbyController.resolveToken(request, xPlayerToken, authHeader);
        GameStateResponse updatedState = gameEngineService.submitInvestment(token, investRequest.amountRappen());
        return ResponseEntity.ok(updatedState);
    }

    @PostMapping("/contract/join")
    public ResponseEntity<GameStateResponse> submitContractJoin(
            @Valid @RequestBody ContractJoinRequest joinRequest,
            HttpServletRequest request,
            @RequestHeader(value = "X-Player-Token", required = false) String xPlayerToken,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        String token = LobbyController.resolveToken(request, xPlayerToken, authHeader);
        GameStateResponse updatedState = gameEngineService.submitContractJoin(token, joinRequest.resolvedChoice());
        return ResponseEntity.ok(updatedState);
    }

    @PostMapping("/contract/leave")
    public ResponseEntity<GameStateResponse> leaveContract(
            HttpServletRequest request,
            @RequestHeader(value = "X-Player-Token", required = false) String xPlayerToken,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        String token = LobbyController.resolveToken(request, xPlayerToken, authHeader);
        GameStateResponse updatedState = gameEngineService.leaveContract(token);
        return ResponseEntity.ok(updatedState);
    }

    @PostMapping("/survey")
    public ResponseEntity<GameStateResponse> submitSurvey(
            @Valid @RequestBody SubmitSurveyRequest surveyRequest,
            HttpServletRequest request,
            @RequestHeader(value = "X-Player-Token", required = false) String xPlayerToken,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        String token = LobbyController.resolveToken(request, xPlayerToken, authHeader);
        GameStateResponse updatedState = gameEngineService.submitSurvey(token, surveyRequest);
        return ResponseEntity.ok(updatedState);
    }
}
