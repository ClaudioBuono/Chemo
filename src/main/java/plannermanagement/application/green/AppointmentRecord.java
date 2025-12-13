package plannermanagement.application.green;

import java.time.LocalDateTime;

public record AppointmentRecord(
        String idPatient,
        String idMedicine,
        LocalDateTime date,
        String chair,
        int duration
) {}
