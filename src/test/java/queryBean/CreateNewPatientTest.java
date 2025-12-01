package queryBean;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import patientmanagement.application.PatientBean;
import patientmanagement.storage.PatientQueryBean;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class CreateNewPatientTest {
    @Mock
    PatientBean patientBean;

    @InjectMocks
    PatientQueryBean patientQueryBean;
    @BeforeEach
     final void setUp() {
        patientQueryBean = new PatientQueryBean();
        patientBean = new PatientBean();
        MockitoAnnotations.openMocks(this);
    }

    //TC_UC_PM_01_2
    @Test
    void testPatientCreationWithIncorrectNameLenght() {
        try {
            Date birthDate = new SimpleDateFormat("dd/MM/yyyy").parse("21/06/1984");
            ObjectId id = new ObjectId();

            when(patientBean.getPatientId()).thenReturn(String.valueOf(id));
            when(patientBean.getTaxCode()).thenReturn("RS5MRAC4H21F8R9S");
            when(patientBean.getName()).thenReturn("");
            when(patientBean.getSurname()).thenReturn("Rossi");
            when(patientBean.getBirthDate()).thenReturn(birthDate);
            when(patientBean.getCity()).thenReturn("Napoli");
            when(patientBean.getPhoneNumber()).thenReturn("3933358960");
            when(patientBean.getStatus()).thenReturn(true);
            when(patientBean.getNotes()).thenReturn("Allergia ad alcuni farmaci");

            assertFalse(patientQueryBean.insertDocument(patientBean));
            patientQueryBean.deleteDocument("_id", patientBean.getPatientId());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }


    //TC_UC_PM_01_3
    @Test
    void testPatientCreationWithIncorrectSurameLenght() {
        try {
            Date birthDate = new SimpleDateFormat("dd/MM/yyyy").parse("21/06/1984");
            ObjectId id = new ObjectId();

            when(patientBean.getPatientId()).thenReturn(String.valueOf(id));
            when(patientBean.getTaxCode()).thenReturn("RS5MRAC4H21F8R9S");
            when(patientBean.getName()).thenReturn("Mario");
            when(patientBean.getSurname()).thenReturn("");
            when(patientBean.getBirthDate()).thenReturn(birthDate);
            when(patientBean.getCity()).thenReturn("Napoli");
            when(patientBean.getPhoneNumber()).thenReturn("3933358960");
            when(patientBean.getStatus()).thenReturn(true);
            when(patientBean.getNotes()).thenReturn("Allergia ad alcuni farmaci");

            assertFalse(patientQueryBean.insertDocument(patientBean));
            patientQueryBean.deleteDocument("_id", patientBean.getPatientId());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    //TC_UC_PM_01_5
    @Test
    void testPatientCreationWithIncorrectBirthDate() {
        try {
            Date birthDate = new SimpleDateFormat("dd/MM/yyyy").parse("30/05/2029");
            ObjectId id = new ObjectId();

            when(patientBean.getPatientId()).thenReturn(String.valueOf(id));
            when(patientBean.getTaxCode()).thenReturn("RS5MRAC4H21F8R9S");
            when(patientBean.getName()).thenReturn("Mario");
            when(patientBean.getSurname()).thenReturn("Rossi");
            when(patientBean.getBirthDate()).thenReturn(birthDate);
            when(patientBean.getCity()).thenReturn("Napoli");
            when(patientBean.getPhoneNumber()).thenReturn("3933358960");
            when(patientBean.getStatus()).thenReturn(true);
            when(patientBean.getNotes()).thenReturn("Allergia ad alcuni farmaci");

            assertFalse(patientQueryBean.insertDocument(patientBean));
            patientQueryBean.deleteDocument("_id", patientBean.getPatientId());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    //TC_UC_PM_01_7
    @Test
    void testPatientCreationWithIncorrectCityLenght() {
        try {
            Date birthDate = new SimpleDateFormat("dd/MM/yyyy").parse("21/06/1984");
            ObjectId id = new ObjectId();

            when(patientBean.getPatientId()).thenReturn(String.valueOf(id));
            when(patientBean.getTaxCode()).thenReturn("RS5MRAC4H21F8R9S");
            when(patientBean.getName()).thenReturn("Mario");
            when(patientBean.getSurname()).thenReturn("Rossi");
            when(patientBean.getBirthDate()).thenReturn(birthDate);
            when(patientBean.getCity()).thenReturn("");
            when(patientBean.getPhoneNumber()).thenReturn("3933358960");
            when(patientBean.getStatus()).thenReturn(true);
            when(patientBean.getNotes()).thenReturn("Allergia ad alcuni farmaci");

            assertFalse(patientQueryBean.insertDocument(patientBean));
            patientQueryBean.deleteDocument("_id", patientBean.getPatientId());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    //TC_UC_PM_01_9
    @Test
    void testPatientCreationWithIncorrectTaxCodeLenght() {
        try {
            Date birthDate = new SimpleDateFormat("dd/MM/yyyy").parse("21/06/1984");
            ObjectId id = new ObjectId();

            when(patientBean.getPatientId()).thenReturn(String.valueOf(id));
            when(patientBean.getTaxCode()).thenReturn("");
            when(patientBean.getName()).thenReturn("Mario");
            when(patientBean.getSurname()).thenReturn("Rossi");
            when(patientBean.getBirthDate()).thenReturn(birthDate);
            when(patientBean.getCity()).thenReturn("Napoli");
            when(patientBean.getPhoneNumber()).thenReturn("3933358960");
            when(patientBean.getStatus()).thenReturn(true);
            when(patientBean.getNotes()).thenReturn("Allergia ad alcuni farmaci");

            assertFalse(patientQueryBean.insertDocument(patientBean));
            patientQueryBean.deleteDocument("_id", patientBean.getPatientId());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    //TC_UC_PM_01_11--> da fare
    @Test
    void testPatientCreationWithIncorrectPhoneNumberLenght() {
        try {
            Date birthDate = new SimpleDateFormat("dd/MM/yyyy").parse("21/06/1984");
            ObjectId id = new ObjectId();

            when(patientBean.getPatientId()).thenReturn(String.valueOf(id));
            when(patientBean.getTaxCode()).thenReturn("RS5MRAC4H21F8R9S");
            when(patientBean.getName()).thenReturn("Mario");
            when(patientBean.getSurname()).thenReturn("Rossi");
            when(patientBean.getBirthDate()).thenReturn(birthDate);
            when(patientBean.getCity()).thenReturn("Napoli");
            when(patientBean.getPhoneNumber()).thenReturn("8968673484668387364874645486416463");
            when(patientBean.getStatus()).thenReturn(true);
            when(patientBean.getNotes()).thenReturn("Allergia ad alcuni farmaci");

            assertFalse(patientQueryBean.insertDocument(patientBean));
            patientQueryBean.deleteDocument("_id", patientBean.getPatientId());
        } catch (ParseException e){
            throw new RuntimeException(e);
        }
    }

    //TC_UC_PM_01_15
    @Test
    void testPatientCreationWithCorrectInput() {
        try {
            Date birthDate = new SimpleDateFormat("dd/MM/yyyy").parse("21/06/1984");
            ObjectId id = new ObjectId();

            when(patientBean.getPatientId()).thenReturn(String.valueOf(id));
            when(patientBean.getTaxCode()).thenReturn("RS5MRAC4H21F8R9S");
            when(patientBean.getName()).thenReturn("Mario");
            when(patientBean.getSurname()).thenReturn("Rossi");
            when(patientBean.getBirthDate()).thenReturn(birthDate);
            when(patientBean.getCity()).thenReturn("Napoli");
            when(patientBean.getPhoneNumber()).thenReturn("3933358960");
            when(patientBean.getStatus()).thenReturn(true);
            when(patientBean.getNotes()).thenReturn("Allergia ad alcuni farmaci");

            assertTrue(patientQueryBean.insertDocument(patientBean));
            patientQueryBean.deleteDocument("_id", patientBean.getPatientId());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

}

