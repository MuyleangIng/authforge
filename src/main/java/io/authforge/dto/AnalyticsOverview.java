package io.authforge.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnalyticsOverview {
    private long totalUsers;
    private long activeUsers;
    private long activeSessions;
    private long totalClients;
    private long loginCount24h;
    private long failedLoginCount24h;
    private long newUsersToday;
}
