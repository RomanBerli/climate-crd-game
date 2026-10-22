package ch.bbw.climategame.controller;

import ch.bbw.climategame.dto.request.JoinLobbyRequest;
import ch.bbw.climategame.dto.response.JoinLobbyResponse;
import ch.bbw.climategame.service.GameEngineService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/lobby")
public class LobbyController {

    public static final String PLAYER_TOKEN_COOKIE = "CLIMATE_PLAYER_TOKEN";

    private final GameEngineService gameEngineService;

    public LobbyController(GameEngineService gameEngineService) {
        this.gameEngineService = gameEngineService;
    }

    @PostMapping("/join")
    public ResponseEntity<JoinLobbyResponse> joinLobby(
            @Valid @RequestBody JoinLobbyRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            @RequestHeader(value = "X-Player-Token", required = false) String headerToken
    ) {
        String existingToken = resolveToken(httpRequest, headerToken, null);
        JoinLobbyResponse response = gameEngineService.joinLobby(request.code(), existingToken);

        ResponseCookie cookie = ResponseCookie.from(PLAYER_TOKEN_COOKIE, response.sessionToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ofHours(24))
                .sameSite("Lax")
                .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/leave")
    public ResponseEntity<Void> leaveLobby(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            @RequestHeader(value = "X-Player-Token", required = false) String headerToken,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        String existingToken = resolveToken(httpRequest, headerToken, authHeader);
        gameEngineService.leaveLobby(existingToken);

        ResponseCookie clearCookie = ResponseCookie.from(PLAYER_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ZERO)
                .sameSite("Lax")
                .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());
        return ResponseEntity.noContent().build();
    }

    public static String resolveToken(HttpServletRequest request, String xPlayerToken, String authHeader) {
        if (xPlayerToken != null && !xPlayerToken.isBlank()) {
            return xPlayerToken.trim();
        }
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String bearer = authHeader.substring(7).trim();
            if (!bearer.isBlank()) {
                return bearer;
            }
        }
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (PLAYER_TOKEN_COOKIE.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
