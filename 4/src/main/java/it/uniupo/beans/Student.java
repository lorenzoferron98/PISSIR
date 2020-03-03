package it.uniupo.beans;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Objects;

/**
 * <p>A class to describe a student.</p>
 * <p>This is an example of Java Bean class.</p>
 *
 * @author Lorenzo Ferron
 * @version 2019.12.13
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Student implements Serializable {

    private Long studentId;
    private String name;
    private String surname;
    private Timestamp dob;
    private String cdl;
    private String aa;
    private Character gender;

    @JsonCreator
    public Student() {
        // Empty body...
    }

    /**
     * @return the id for this student.
     */
    @JsonProperty
    public Long getStudentId() {
        return studentId;
    }

    /**
     * Sets a new id for this student.
     *
     * @param studentId the new id
     */
    @JsonIgnore
    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    /**
     * @return the name for this student
     */
    public String getName() {
        return name;
    }

    /**
     * Sets a new name for this student.
     *
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the new surname for this student
     */
    public String getSurname() {
        return surname;
    }

    /**
     * Sets a new surname for this student.
     *
     * @param surname the new surname
     */
    public void setSurname(String surname) {
        this.surname = surname;
    }

    /**
     * @return the date of birth for this student.
     */
    public Timestamp getDob() {
        return dob;
    }

    /**
     * Sets a new date of birth for this student.
     *
     * @param dob the new date of birth
     */
    public void setDob(Timestamp dob) {
        this.dob = dob;
    }

    /**
     * @return the corso di laurea for this student.
     */
    public String getCdl() {
        return cdl;
    }

    /**
     * Sets a new corso di laurea for this student.
     *
     * @param cdl the new corso di laurea
     */
    public void setCdl(String cdl) {
        this.cdl = cdl;
    }

    /**
     * @return the anno accademico for this student.
     */
    public String getAa() {
        return aa;
    }

    /**
     * Sets a new anno accademico for this student.
     *
     * @param aa the new anno accademico
     */
    public void setAa(String aa) {
        this.aa = aa;
    }

    /**
     * @return the gender for this student.
     */
    public Character getGender() {
        return gender;
    }

    /**
     * Sets a  new gender for this student.
     *
     * @param gender the new gender
     */
    public void setGender(Character gender) {
        this.gender = gender;
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentId, name, surname, aa, cdl, dob, gender);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        else if (obj instanceof Student) {
            Student anotherStudent = (Student) obj;
            return anotherStudent.studentId.equals(this.studentId)
                    && anotherStudent.name.equals(this.name)
                    && anotherStudent.surname.equals(this.surname)
                    && anotherStudent.aa.equals(this.aa)
                    && anotherStudent.cdl.equals(this.cdl)
                    && anotherStudent.dob.equals(this.dob)
                    && anotherStudent.gender.equals(this.gender);
        }
        return false;
    }

    @Override
    public String toString() {
        return "id: " + this.studentId + "\n" +
                "nome: " + this.name + "\n" +
                "cognome: " + this.surname + "\n" +
                "CDL: " + this.cdl + "\n" +
                "A.A.: " + this.aa + "\n" +
                "Sesso: " + this.gender + "\n" +
                "Data di nascita: " + this.dob;
    }

}
