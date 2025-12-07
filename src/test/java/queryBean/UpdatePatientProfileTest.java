package queryBean;

import connector.DatabaseConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import patientmanagement.application.TherapyBean;
import patientmanagement.application.TherapyMedicineBean;
import patientmanagement.storage.PatientQueryBean;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdatePatientProfileTest {
    @Mock
    TherapyBean therapyBean;
    @Mock
    TherapyMedicineBean therapyMedicineBean;
    @Mock
    ArrayList<TherapyMedicineBean> meds;
    @InjectMocks
    PatientQueryBean patientQueryBean;

    @BeforeEach
    final void setUp(){
        DatabaseConnector.setDbName("Chemo_TEST");
        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() {
        DatabaseConnector.setDbName("Chemo");
    }

    //TC_UC_PM_05_3
    @Test
    void testUpdatePatientWithIncorrectConditionLength(){
        assertFalse(patientQueryBean.updateDocument("_id", "63de1815d74b4d04cf039760", "condition", ""));
    }

    //TC_UC_PM_05_7
    @Test
    void testUpdatePatientWithIncorrectTherapyDose(){
        assertFalse(patientQueryBean.updateDocument("_id", "63de1815d74b4d04cf039760", "dose", 0));
    }

    //TC_UC_PM_05_8
    @Test
    void testUpdatePatientWithIncorrectTherapySession(){
        assertTrue(patientQueryBean.updateDocument("_id", "63de1815d74b4d04cf039760", "session", 0));
    }

    //TC_UC_PM_05_9
    @Test
    void testUpdatePatientWithIncorrectTherapyDuration(){
        assertFalse(patientQueryBean.updateDocument("_id", "63de1815d74b4d04cf039760", "duration", 0));
    }

    //TC_UC_PM_05_10
    @Test
    void testUpdatePatientWithIncorrectTherapyFrequency(){
        assertFalse(patientQueryBean.updateDocument("_id", "63de1815d74b4d04cf039760", "frequency", 0));
    }
}
