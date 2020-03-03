package rpc.common;

import it.uniupo.beans.Student;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;
import java.util.Map;

/**
 * An interface to describe a remote API.
 *
 * @author Lorenzo Ferron
 * @version 2020.02.23
 */
public interface StudentBasicApi {

    @Headers("Accept: application/json;charset=UTF-8")
    @GET("/students")
    Call<List<Student>> getAll(@QueryMap Map<String, String> filters);

    @Headers("Accept: application/json;charset=UTF-8")
    @POST("/students")
    Call<Student> create(@Body Student student);

    @Headers("Accept: application/json;charset=UTF-8")
    @DELETE("/students/{id}")
    Call<Student> delete(@Path("id") Long studentId);
    //Call<Response<Void>> delete(@Path("id") Long studentId);

    @Headers("Accept: application/json;charset=UTF-8")
    @GET("/students/{id}")
    Call<Student> getOne(@Path("id") Long studentId);

    @Headers("Accept: application/json;charset=UTF-8")
    @PATCH("/students/{id}")
    Call<Student> update(@Path("id") Long studentId, @Body Student student);

}
