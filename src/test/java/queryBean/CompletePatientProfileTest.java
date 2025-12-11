package queryBean;

import connector.DatabaseConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import patientmanagement.application.TherapyBean;
import patientmanagement.application.TherapyMedicineBean;
import patientmanagement.storage.PatientQueryBean;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class CompletePatientProfileTest {
    @Mock
    TherapyMedicineBean therapyMedicineBean;
    @Mock
    ArrayList<TherapyMedicineBean> meds;
    @Mock
    TherapyBean therapyBean;

    @InjectMocks
    PatientQueryBean patientQueryBean;

    @BeforeEach
    void setUp() {
        DatabaseConnector.setDbName("Chemo_TEST");
        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() {
        DatabaseConnector.setDbName("Chemo");
    }

    //TC_UC_PM_05_10
    @Test
    void testCorrectCompletePatientProfile(){
        therapyMedicineBean = new TherapyMedicineBean();
        meds = new ArrayList<>();
        meds.add(therapyMedicineBean);

        when(therapyBean.getSessions()).thenReturn(10);
        when(therapyBean.getMedicines()).thenReturn(meds);
        when(therapyBean.getDuration()).thenReturn(60);
        when(therapyBean.getFrequency()).thenReturn(2);

        assertFalse(patientQueryBean.insertDocument(therapyBean, "63dd3eac74c697331b1f019a"));
        assertTrue(patientQueryBean.updateDocument("_id", "63dd3eac74c697331b1f019a", "condition", "Tumore al cervello"));
    }

    //TC_UC_PM_05_3
    @Test
    void testIncorrectConditionLength(){
        therapyMedicineBean = new TherapyMedicineBean();
        meds = new ArrayList<>();
        meds.add(therapyMedicineBean);

        when(therapyBean.getSessions()).thenReturn(10);
        when(therapyBean.getMedicines()).thenReturn(meds);
        when(therapyBean.getDuration()).thenReturn(60);
        when(therapyBean.getFrequency()).thenReturn(2);

        assertFalse(patientQueryBean.insertDocument(therapyBean, "63dd3eac74c697331b1f019a"));
        assertFalse(patientQueryBean.updateDocument("_id", "63dd3eac74c697331b1f019a", "condition", ""));
    }

    //TC_UC_PM_05_8
    //TC_UC_PM_05_9
    //TC_UC_PM_05_10
    @ParameterizedTest(name = "Test inserimento fallito con: sessions={0}, duration={1}, frequency={2}")
    @CsvSource({
            "0,  60, 2",   // //TC_UC_PM_05_8: Sessioni errate (0), il resto OK
            "10, 0,  2",   // //TC_UC_PM_05_9: Durata errata (0), il resto OK
            "10, 60, 0"    // //TC_UC_PM_05_10: Frequenza errata (0), il resto OK
    })
    void testIncorrectTherapyValues(int sessions, int duration, int frequency) {
        therapyMedicineBean = new TherapyMedicineBean();
        meds = new ArrayList<>();
        meds.add(therapyMedicineBean);

        when(therapyBean.getSessions()).thenReturn(sessions);
        when(therapyBean.getDuration()).thenReturn(duration);
        when(therapyBean.getFrequency()).thenReturn(frequency);
        when(therapyBean.getMedicines()).thenReturn(meds);

        assertFalse(patientQueryBean.insertDocument(therapyBean, "63dd3eac74c697331b1f019a"));

        assertTrue(patientQueryBean.updateDocument("_id", "63dd3eac74c697331b1f019a", "condition", "Tumore al cervello"));
    }
}