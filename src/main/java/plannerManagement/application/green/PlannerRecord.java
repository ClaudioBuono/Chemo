package plannerManagement.application.green;

import java.time.LocalDateTime;
import java.util.List;

public record PlannerRecord(
        String id,
        LocalDateTime startDate,
        LocalDateTime endDate,
        List<AppointmentRecord> appointments
) {}
