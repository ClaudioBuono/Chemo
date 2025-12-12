package plannerManagement.application.green;

import java.time.LocalDateTime;
import java.util.ArrayList;

public record PlannerRecord(
        String id,
        LocalDateTime startDate,
        LocalDateTime endDate,
        ArrayList<AppointmentRecord>appointments
) {}
