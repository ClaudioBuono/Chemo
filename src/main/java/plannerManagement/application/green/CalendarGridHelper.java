package plannerManagement.application.green;

import plannerManagement.application.AppointmentBean;
import plannerManagement.application.PlannerBean;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Helper class for managing the calendar grid generation.
 * This class isolates the business logic from the Servlet/JSP layer to enable
 * proper microbenchmarking with JMH.
 * * It implements two algorithms:
 * 1. Legacy: The original inefficient approach found in the JSP.
 * 2. Green: An optimized approach using HashMaps and Streams.
 */
public class CalendarGridHelper {

    // Formatter used to generate the unique Map key for each grid cell (e.g., "2023-10-25_09:00")
    // Static allocation avoids creating new objects for every call (Memory efficiency).
    private static final DateTimeFormatter GRID_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:00");

    /**
     * LEGACY METHOD (BASELINE)
     * * Simulates the heavy algorithm currently used in the JSP.
     * It iterates over every cell of the grid (Days x Hours) and, for each cell,
     * it iterates over the entire appointment list.
     * * @param planner JavaBean containing the data.
     * @return The number of matches found (dummy return value for JMH consumption).
     */
    public int countLegacyMatches(final PlannerBean planner) {
        int matches = 0;

        // Simulates the visible grid: 5 days (columns) x 9 hours (rows) = 45 cells.
        // This mirrors the nested "for" loops in the view layer.
        for (int day = 0; day < 5; ++day) {
            for (int hour = 9; hour < 18; ++hour) {

                if (planner.getAppointments() != null) {
                    // THE BOTTLENECK:
                    // Iterating through the entire list for every single cell.
                    // If the list has N elements and the grid has M cells, complexity is O(N*M).
                    for (final AppointmentBean app : planner.getAppointments()) {

                        // Simulating the CPU cost of the JSP scriptlet:
                        // "LocalDate.ofInstant(appointment.getDate().toInstant(), ZoneId.of...)"
                        // Repeated Date/Time conversions inside a loop are CPU-intensive.
                        if (app.getDate() != null) {
                            final LocalDateTime appTime = LocalDateTime.ofInstant(
                                    app.getDate().toInstant(),
                                    ZoneId.of("Europe/Rome")
                            );

                            // Dummy check to simulate the JSP conditional logic
                            if (appTime.getHour() == hour) {
                                ++matches;
                            }
                        }
                    }
                }
            }
        }
        return matches;
    }

    /**
     * GREEN METHOD
     * * Uses Java Streams and HashMaps to group appointments in a single pass.
     * It eliminates nested loops and redundant date conversions, drastically reducing
     * CPU cycles and energy consumption.
     * * @param planner The immutable Record containing the data.
     * @return A Map where Key is the time slot and Value is the list of appointments.
     */
    public Map<String, List<AppointmentRecord>> preCalculateGrid(final PlannerRecord planner) {
        // Fail-fast to avoid unnecessary processing
        if (planner.appointments() == null || planner.appointments().isEmpty()) {
            return new HashMap<>();
        }

        // Efficient grouping using Stream API.
        // We traverse the list only ONCE and compute the unique slot key.
        // We round the minutes to the hour (e.g. 09:15 -> 09:00) to act as a bucket.
        return planner.appointments().stream()
                .filter(app -> app.date() != null) // Safety check
                .collect(Collectors.groupingBy(app -> app.date().format(GRID_KEY_FORMATTER)));
    }
}