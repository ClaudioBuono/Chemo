package queryBean;

import medicinemanagement.application.MedicineBean;
import medicinemanagement.storage.MedicineQueryBean;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class AddNewMedicineTest {

    @Mock
    MedicineBean medicineBean;

    @InjectMocks
    MedicineQueryBean medicineQueryBean;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this); // initMocks è vecchio, usa openMocks
    }

    //TC_UC_PM_03_6
    @Test
    void testAddCorrectMedicine(){
        ObjectId id = new ObjectId();
        when(medicineBean.getId()).thenReturn(String.valueOf(id));
        when(medicineBean.getName()).thenReturn("Dexorubicina");
        when(medicineBean.getIngredients()).thenReturn("Doxorubicina cloridrato");
        when(medicineBean.getAmount()).thenReturn(5);
        when(medicineBean.getPackages()).thenReturn(null);

        assertTrue(medicineQueryBean.insertDocument(medicineBean));

        medicineQueryBean.deleteDocument("_id", String.valueOf(id));
    }
}