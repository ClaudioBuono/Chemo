package usermanagement.application;

import java.io.Serializable;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UserBean implements Serializable {
    private String id;
    private String name;
    private String surname;
    private Date birthDate;
    private String birthplace;
    private String username;
    private String password;
    private String specialization;
    private int type;

    public UserBean(){}

    public UserBean(String id, String name, String surname, Date birthDate, String birthplace, String username, String password, String specialization, int type) {
        this.id = id;
        this.name = name;
        this.surname = surname;
        this.birthDate = birthDate;
        this.birthplace = birthplace;
        this.username = username;
        this.password = password;
        this.specialization = specialization;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public String getParsedBirthDate() {
        return dateParser(birthDate);
    }

    public String getBirthplace() {
        return birthplace;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getSpecialization() {
        return specialization;
    }

    public int getType() {
        return type;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setSurname(final String surname) {
        this.surname = surname;
    }

    public void setBirthDate(final Date birthDate) {
        this.birthDate = birthDate;
    }

    public void setBirthplace(final String birthplace) {
        this.birthplace = birthplace;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public void setSpecialization(final String specialization) {
        this.specialization = specialization;
    }

    public void setType(final int type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Utente{" +
                "id='" + id + '\'' +
                ", nome='" + name + '\'' +
                ", cognome='" + surname + '\'' +
                ", dataNascita=" + birthDate +
                ", cittaNascita='" + birthplace + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", specializzazione='" + specialization + '\'' +
                ", tipo=" + type +
                '}';
    }

    private String dateParser(final Date date) {
        final Format formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(date);
    }
}
