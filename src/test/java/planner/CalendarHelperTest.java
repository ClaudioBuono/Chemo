package planner;

import org.junit.jupiter.api.Test;
import plannermanagement.application.green.AppointmentRecord;
import plannermanagement.application.green.CalendarGridHelper;
import plannermanagement.application.green.PlannerRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalendarHelperTest {

    @Test
    void testGreenAlgorithmLogic() {
        AppointmentRecord app = new AppointmentRecord(
                "id1",
                "id2",
                LocalDateTime.of(2023, 10, 25, 9, 15),
                "60",
                60
        );

        // Planner che copre quella settimana
        PlannerRecord planner = new PlannerRecord(
                "p1",
                LocalDateTime.of(2023, 10, 23, 0, 0 , 0), // Lunedì
                LocalDateTime.of(2023, 10, 27,0,0,0), // Venerdì
                List.of(app)
        );

        // 2. ACT: Eseguiamo l'algoritmo Green
        CalendarGridHelper helper = new CalendarGridHelper();
        Map<String, List<AppointmentRecord>> risultato = helper.preCalculateGrid(planner);

        // 3. ASSERT: Verifichiamo che l'appuntamento sia finito nella cella giusta
        // La chiave deve essere arrotondata all'ora: "2023-10-25_09:00"
        String chiaveAttesa = "2023-10-25_09:00";

        assertTrue(risultato.containsKey(chiaveAttesa), "La griglia deve contenere la chiave oraria");
        assertEquals(1, risultato.get(chiaveAttesa).size(), "Deve esserci 1 appuntamento in quella cella");
        assertEquals("id1", risultato.get(chiaveAttesa).get(0).idPatient(), "Il paziente deve essere Mario Rossi");
    }
}
