package ch.bbw.climategame.controller;

import ch.bbw.climategame.config.AppProperties;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final AppProperties appProperties;

    public AdminController(AdminService adminService, AppProperties appProperties) {
        this.adminService = adminService;
        this.appProperties = appProperties;
    }

    @PostMapping("/sessions")
    public ResponseEntity<AdminSessionResponse> createSession(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @Valid @RequestBody CreateSessionRequest request
    ) {
        verifyAdminKey(adminKey, null);
        AdminSessionResponse created = adminService.createSession(request.riskLevel());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<AdminSessionResponse>> getAllSessions(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey
    ) {
        verifyAdminKey(adminKey, null);
        return ResponseEntity.ok(adminService.getAllSessions());
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<AdminSessionResponse> getSession(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @PathVariable UUID id
    ) {
        verifyAdminKey(adminKey, null);
        return ResponseEntity.ok(adminService.getSession(id));
    }

    @PostMapping("/sessions/{id}/start")
    public ResponseEntity<AdminSessionResponse> startSession(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @PathVariable UUID id
    ) {
        verifyAdminKey(adminKey, null);
        return ResponseEntity.ok(adminService.startSession(id));
    }

    @PostMapping("/sessions/{id}/next-round")
    public ResponseEntity<AdminSessionResponse> advanceRound(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @PathVariable UUID id
    ) {
        verifyAdminKey(adminKey, null);
        return ResponseEntity.ok(adminService.advanceRound(id));
    }

    @GetMapping(value = "/sessions/{id}/export.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> exportSessionCsv(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @RequestParam(value = "adminKey", required = false) String queryAdminKey,
            @PathVariable UUID id
    ) {
        verifyAdminKey(adminKey, queryAdminKey);
        AdminSessionResponse session = adminService.getSession(id);
        String csvContent = adminService.exportSessionCsv(id);
        String filename = "milinski_session_" + session.code() + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csvContent.getBytes(StandardCharsets.UTF_8));
    }

    private void verifyAdminKey(String headerKey, String queryKey) {
        String expected = appProperties.getAdminSecret();
        String provided = (headerKey != null && !headerKey.isBlank()) ? headerKey : queryKey;
        if (provided == null || !expected.equals(provided)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Ungültiger Admin-Schlüssel (X-Admin-Key).");
        }
    }
}
