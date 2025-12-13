package plannerManagement.benchmark;

import org.openjdk.jmh.annotations.*;
import plannerManagement.application.AppointmentBean;
import plannerManagement.application.PlannerBean;
import plannerManagement.application.green.AppointmentRecord;
import plannerManagement.application.green.CalendarGridHelper;
import plannerManagement.application.green.PlannerRecord;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class CalendarBenchmark {

    private CalendarGridHelper helper;
    private PlannerBean legacyPlanner;
    private PlannerRecord greenPlanner;

    @Setup(Level.Trial)
    public void setup() {
        helper = new CalendarGridHelper();

        // --- 1. SETUP LEGACY (Bean Mutabili + Date) ---
        legacyPlanner = new PlannerBean();
        ArrayList<AppointmentBean> legacyApps = new ArrayList<>();

        for(int i = 0; i < 100; ++i) {
            // Generiamo date fittizie sparse nella settimana
            LocalDateTime now = LocalDateTime.now().plusHours(i % 20);
            Date legacyDate = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());

            // Uso i Setter come nel tuo Bean originale
            AppointmentBean bean = new AppointmentBean();
            bean.setIdPatient("pat_" + i);
            bean.setIdMedicine("med_" + i);
            bean.setDate(legacyDate);
            bean.setChair("A" + (i % 5));
            bean.setDuration(60);

            legacyApps.add(bean);
        }
        legacyPlanner.setAppointments(legacyApps);

        // --- 2. SETUP GREEN (Record Immutabili + LocalDateTime) ---
        List<AppointmentRecord> greenApps = new ArrayList<>();

        for(int i=0; i<100; i++) {
            LocalDateTime now = LocalDateTime.now().plusHours(i % 20);

            // Costruttore canonico del Record
            greenApps.add(new AppointmentRecord(
                    "pat_" + i,  // idPatient
                    "med_" + i,  // idMedicine
                    now,         // LocalDateTime date
                    "A" + (i%5), // chair
                    60           // duration
            ));
        }
        greenPlanner = new PlannerRecord("plan1", LocalDateTime.now(), LocalDateTime.now().plusDays(5), greenApps);
    }

    // --- BENCHMARK ---

    @Benchmark
    public int testLegacy() {
        return helper.countLegacyMatches(legacyPlanner);
    }

    @Benchmark
    public void testGreen() {
        helper.preCalculateGrid(greenPlanner);
    }
}