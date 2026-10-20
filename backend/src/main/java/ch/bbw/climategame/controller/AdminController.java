package ch.bbw.climategame.controller;

import ch.bbw.climategame.dto.request.CreateSessionRequest;
import ch.bbw.climategame.dto.response.AdminSessionResponse;
import ch.bbw.climategame.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Alle Endpunkte unter /api/admin/** sind durch Spring Security (SecurityConfig & AdminAuthenticationFilter)
 * geschützt und erfordern die Rolle ROLE_ADMIN (authentifiziert über APP_ADMIN_SECRET).
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/sessions")
    public ResponseEntity<AdminSessionResponse> createSession(
            @Valid @RequestBody CreateSessionRequest request
    ) {
        AdminSessionResponse created = adminService.createSession(request.riskLevel());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<AdminSessionResponse>> getAllSessions() {
        return ResponseEntity.ok(adminService.getAllSessions());
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<AdminSessionResponse> getSession(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.getSession(id));
    }

    @PostMapping("/sessions/{id}/start")
    public ResponseEntity<AdminSessionResponse> startSession(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.startSession(id));
    }

    @PostMapping("/sessions/{id}/next-round")
    public ResponseEntity<AdminSessionResponse> advanceRound(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.advanceRound(id));
    }

    @GetMapping(value = "/sessions/{id}/export.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> exportSessionCsv(@PathVariable UUID id) {
        AdminSessionResponse session = adminService.getSession(id);
        String csvContent = adminService.exportSessionCsv(id);
        String filename = "milinski_session_" + session.code() + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csvContent.getBytes(StandardCharsets.UTF_8));
    }
}
