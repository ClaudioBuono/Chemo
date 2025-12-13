package plannermanagement.application.green;

import java.time.LocalDateTime;

public record PlannerSummary (
        String id,
        LocalDateTime startDate,
        LocalDateTime endDate
) {}
