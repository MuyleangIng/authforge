package io.authforge.controller;

import io.authforge.service.AnalyticsService;
import io.authforge.service.RealmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/realms/{realm}/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('admin')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final RealmService realmService;

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview(@PathVariable String realm) {
        UUID realmId = realmService.findByName(realm).getId();
        return ResponseEntity.ok(analyticsService.getOverview(realmId));
    }

    @GetMapping("/logins")
    public ResponseEntity<Map<String, Object>> getLogins(@PathVariable String realm,
            @RequestParam(defaultValue = "7") int days) {
        UUID realmId = realmService.findByName(realm).getId();
        return ResponseEntity.ok(analyticsService.getLoginStats(realmId, days));
    }

    @GetMapping("/active-sessions")
    public ResponseEntity<Map<String, Object>> getActiveSessions(@PathVariable String realm) {
        UUID realmId = realmService.findByName(realm).getId();
        return ResponseEntity.ok(analyticsService.getActiveSessionStats(realmId));
    }
}
