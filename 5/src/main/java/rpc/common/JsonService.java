package rpc.common;

import it.uniupo.beans.Student;

import java.util.Map;

/**
 * An interface to call remote methods.
 *
 * @author Lorenzo Ferron
 * @version 2020.02.23
 */
public interface JsonService {

    String getAll(Map<String, String> filters) throws Exception;

    Student create(Student student) throws Exception;

    Student delete(Long studentId) throws Exception;

    Student getOne(Long studentId) throws Exception;

    Student update(Long studentId, Student student) throws Exception;

}
