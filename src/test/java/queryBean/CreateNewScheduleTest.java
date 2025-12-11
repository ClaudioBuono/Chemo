package queryBean;

import connector.DatabaseConnector;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import plannerManagement.application.AppointmentBean;
import plannerManagement.application.PlannerBean;
import plannerManagement.storage.PlannerQueryBean;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class CreateNewScheduleTest {
    @Mock
    PlannerBean plannerBean;
    @Mock
    ArrayList<AppointmentBean> appointments;
    @Mock
    AppointmentBean appointmentBean;
    @InjectMocks
    PlannerQueryBean plannerQueryBean;

    @BeforeEach
    void setUp(){
        DatabaseConnector.setDbName("Chemo_TEST");
        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() {
        DatabaseConnector.setDbName("Chemo");
    }

    //TC_UC_PM_03_2
    @Test
    void testCreateCorrectNewSchedule(){
        try {
            Date startDate = new SimpleDateFormat("dd/MM/yyyy").parse("04/02/2023");
            Date endDate = new SimpleDateFormat("dd/MM/yyyy").parse("11/02/2023");
            appointmentBean = new AppointmentBean();
            appointments = new ArrayList<>();
            appointments.add(appointmentBean);
            ObjectId id = new ObjectId();

            when(plannerBean.getId()).thenReturn(String.valueOf(id));
            when(plannerBean.getStartDate()).thenReturn(startDate);
            when(plannerBean.getEndDate()).thenReturn(endDate);
            when(plannerBean.getAppointments()).thenReturn(appointments);

            assertTrue(plannerQueryBean.insertDocument(plannerBean));
            plannerQueryBean.deleteDocument("_id", plannerBean.getId());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

    }

    //TC_UC_PM_03_1
    @Test
    void testCreateScheduleWithNoAppointments(){
        try {
            Date startDate = new SimpleDateFormat("dd/MM/yyyy").parse("04/02/2023");
            Date endDate = new SimpleDateFormat("dd/MM/yyyy").parse("11/02/2023");
            appointmentBean = new AppointmentBean();
            appointments = new ArrayList<>();
            ObjectId id = new ObjectId();

            when(plannerBean.getId()).thenReturn(String.valueOf(id));
            when(plannerBean.getStartDate()).thenReturn(startDate);
            when(plannerBean.getEndDate()).thenReturn(endDate);
            when(plannerBean.getAppointments()).thenReturn(appointments);

            assertFalse(plannerQueryBean.insertDocument(plannerBean));
            plannerQueryBean.deleteDocument("_id", plannerBean.getId());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

    }
}
