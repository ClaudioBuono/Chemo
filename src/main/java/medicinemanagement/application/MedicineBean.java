package medicinemanagement.application;

import java.util.ArrayList;
import java.util.List;

public class MedicineBean {

    //Parametri
    private String id;
    private String name;
    private String ingredients;
    private int amount;
    private ArrayList<PackageBean> packages;


    //Costruttori
    public MedicineBean() {
        packages = new ArrayList<>();
    }

    public MedicineBean(String name, String ingredients) {
        this.name = name;
        this.ingredients = ingredients;
        this.amount = 0;
        this.packages = new ArrayList<>();
    }

    public MedicineBean(String id, String name, String ingredients, int amount, ArrayList<PackageBean> packages) {
        this.id = id;
        this.name = name;
        this.ingredients = ingredients;
        this.amount = amount;
        this.packages = packages;
    }

    //Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIngredients() {
        return ingredients;
    }

    public int getAmount() {
        return amount;
    }

    public PackageBean getPackage(final int index) {
        return packages.get(index);
    }

    public List<PackageBean> getPackages() {
        return packages;
    }

    //Setters
    public void setId(final String id) {
        this.id = id;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setIngredients(final String ingredients) {
        this.ingredients = ingredients;
    }

    public void setAmount(final int amount) {
        this.amount = amount;
    }

    public void setPackages(final List<PackageBean> packages) {
        this.packages = (ArrayList<PackageBean>) packages;
    }

    //Metodi ereditati da Object
    @Override
    public String toString() {
        return "MedicineBean{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", ingredients='" + ingredients + '\'' +
                ", amount=" + amount +
                ", packages=" + packages +
                '}';
    }

    //Altri metodi
    public void addPackage(final PackageBean newPackage) {
        this.packages.add(newPackage);
    }
}
