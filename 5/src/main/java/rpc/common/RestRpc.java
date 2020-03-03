package rpc.common;

import com.google.gson.Gson;
import it.uniupo.beans.Student;

import java.util.List;
import java.util.Map;

public class RestRpc implements JsonService {

    private final StudentBasicApi studentApi;

    public RestRpc(StudentBasicApi studentApi) {
        this.studentApi = studentApi;
    }

    @Override
    public String getAll(Map<String, String> filters) throws Exception {
        List<Student> students = studentApi.getAll(filters).execute().body();
        Gson gson = new Gson();
        return gson.toJson(students);
    }

    @Override
    public Student create(Student student) throws Exception {
        return studentApi.create(student).execute().body();
    }

    @Override
    public Student delete(Long studentId) throws Exception {
        return studentApi.delete(studentId).execute().body();
    }

    @Override
    public Student getOne(Long studentId) throws Exception {
        return studentApi.getOne(studentId).execute().body();
    }

    @Override
    public Student update(Long studentId, Student student) throws Exception {
        return studentApi.update(studentId, student).execute().body();
    }
}
