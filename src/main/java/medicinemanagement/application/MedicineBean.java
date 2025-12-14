package medicinemanagement.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        final MedicineBean that = (MedicineBean) o;
        return amount == that.amount &&
                Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(ingredients, that.ingredients) &&
                Objects.equals(packages, that.packages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, ingredients, amount, packages);
    }

    //Altri metodi
    public void addPackage(final PackageBean newPackage) {
        this.packages.add(newPackage);
    }
}
