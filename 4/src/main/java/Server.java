import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.net.MediaType;
import io.javalin.Javalin;
import io.javalin.apibuilder.CrudHandler;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.InternalServerErrorResponse;
import io.javalin.plugin.openapi.OpenApiOptions;
import io.javalin.plugin.openapi.OpenApiPlugin;
import io.javalin.plugin.openapi.annotations.*;
import io.javalin.plugin.openapi.ui.SwaggerOptions;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import it.uniupo.beans.Student;
import org.apache.commons.cli.*;
import org.eclipse.jetty.http.HttpStatus;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;

import static io.javalin.apibuilder.ApiBuilder.crud;

/**
 * A class to start a new instance server.
 *
 * @author Lorenzo Ferron
 * @version 2019.12.12
 */
public class Server {

    // https://semver.org/lang/it/
    private static final String MAJOR = "1";
    private static final String MINOR = "0";
    private static final String PATCH = "0";

    private static final int DEFAULT_PORT = 9000;

    /**
     * Connection of this server.
     */
    public static Connection connection = null;

    public static void main(String[] args) {
        Options options = new Options();

        Option port = new Option("p", "port", true, "port number");
        port.setArgs(1);
        port.setArgName("");
        options.addOption(port);

        Option help = new Option("h", "help", false, "show this help");
        options.addOption(help);

        Option version = new Option("v", "version", false, "show version");
        options.addOption(version);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        boolean inMemoryDB = false;
        try {
            cmd = parser.parse(options, args);

            if (cmd.hasOption("help"))
                usage(formatter, options, 0);
            else if (cmd.hasOption("version")) {
                System.out.println(MAJOR + "." + MINOR + "." + PATCH);
                System.exit(0);
            }

            // create a database connection
            if (cmd.getArgs().length == 1) {
                File dbOnFile = new File(cmd.getArgs()[0]);
                if (!dbOnFile.exists()) {
                    System.err.println("No such file found!");
                    System.exit(1);
                }
                connection = DriverManager.getConnection("jdbc:sqlite:" + dbOnFile.getAbsolutePath());
            } else if (cmd.getArgs().length == 0) {
                System.out.println("In-Memory DB will be created");
                connection = DriverManager.getConnection("jdbc:sqlite::memory:");
                PreparedStatement stmt = connection.prepareStatement("create table student\n" +
                        "(\n" +
                        "    student_id INTEGER\n" +
                        "        primary key autoincrement,\n" +
                        "    name       TEXT    not null,\n" +
                        "    surname    TEXT    not null,\n" +
                        "    dob        INTEGER not null,\n" +
                        "    cdl        TEXT    not null,\n" +
                        "    aa         TEXT    not null,\n" +
                        "    gender     TEXT default 'M' not null,\n" +
                        "    check (length(gender) == 1)\n" +
                        ");");
                stmt.executeUpdate();
                inMemoryDB = true;
            } else usage(formatter, options, 1);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            usage(formatter, options, 1);
        }
        assert connection != null;
        assert cmd != null;

        Javalin app = Javalin.create(javalinConfig -> {
            javalinConfig.showJavalinBanner = false;
            javalinConfig.enableDevLogging();
            javalinConfig.registerPlugin(new OpenApiPlugin(getOpenApiOptions()));
        }).start(cmd.hasOption("port") ? Integer.parseInt(cmd.getOptionValue("port")) : DEFAULT_PORT);
        assert app != null;

        Runtime.getRuntime().addShutdownHook(new Thread(app::stop));

        boolean finalInMemoryDB = inMemoryDB;
        app.events(eventListener -> eventListener.serverStopped(() -> {

            if (finalInMemoryDB) {
                File tmpFile = File.createTempFile("backup-test", ".sqlite");

                Statement stmt = connection.createStatement();
                stmt.executeUpdate("backup to " + tmpFile.getAbsolutePath());
                System.out.println("Dump DB in " + tmpFile.getAbsolutePath());
            }

            System.out.print("Closing DB connection... ");
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException e) {
                // connection close failed
                System.out.println("FAIL");
                e.printStackTrace();
                Runtime.getRuntime().halt(1);
            }
            System.out.println("OK");
            System.out.println("Bye!");
            Runtime.getRuntime().halt(0);
        }));

        app.get("/test", ctx -> ctx.html("<h1>it.uniupo.Server works!</h1>"));

        app.routes(() -> crud("students/:student-id", new StudentController()));
        app.exception(Exception.class, (exception, ctx) -> ctx.status(HttpStatus.INTERNAL_SERVER_ERROR_500));
    }

    private static void usage(HelpFormatter formatter, Options options, int status) {
        formatter.printHelp("[db]", "API REST server", options, null, true);
        System.exit(status);
    }

    private static OpenApiOptions getOpenApiOptions() {
        Info applicationInfo = new Info()
                .contact(
                        new Contact()
                                .name("Lorenzo Ferron")
                                .email("20024182@studenti.uniupo.it")
                                .url("https://github.com/lorenzoferron98/")
                )
                .title("Esercizio REST")
                .termsOfService("http://example.com/terms/")
                .license(
                        new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")
                )
                .version(MAJOR + "." + MINOR + "." + PATCH)
                .description(
                        "<p>Usando l’httpserver introdotto a lezione, sviluppare un semplice servizio <br/>" +
                                "Basato su API REST che consenta di gestire un repository di informazioni. <br/>" +
                                "Per semplicità supponiamo che le informazioni siano relativi a una scuola <br/>" +
                                "La risorsa è lo studente caratterizzato dagli opportuni parametri per <br/>" +
                                "inquadrarlo nel corso e nell’anno di studi del suo curriculum.</p>" +
                                "<p>Le seguenti funzionalità devono essere gestite: " +
                                "<ol>" +
                                "<li>Il deposito di una risorsa nel repository</li>" +
                                "<li>La sovrascrittura di una risorsa</li>" +
                                "<li>La cancellazione di una risorsa</li>" +
                                "<li>Il retrieval di una o più risorse per ID oppure usando un Query su un " +
                                "<br/>URI" +
                                "</li>" +
                                "</ol></p>" +
                                "<p>Il sistema deve poter gestire due linguaggi di rappresentazione: XML, JSON</p>"
                );
        return new OpenApiOptions(applicationInfo)
                .path("/swagger-docs")
                .swagger(
                        new SwaggerOptions("/swagger")
                                .title("Documentazione Esercizio 4")
                );
    }

    private static class StudentController implements CrudHandler {
        private XmlMapper xmlMapper = new XmlMapper();

        @OpenApi(
                responses = {
                        @OpenApiResponse(
                                status = "200",
                                content = {
                                        @OpenApiContent(from = Student.class, type = "application/json;charset=UTF-8"),
                                        @OpenApiContent(from = Student.class, type = "text/xml;charset=UTF-8")
                                },
                                description = "Request successful")
                },
                requestBody =
                @OpenApiRequestBody(content = {
                        @OpenApiContent(from = Student.class, type = "application/json;charset=UTF-8"),
                        @OpenApiContent(from = Student.class, type = "text/xml;charset=UTF-8")
                }, required = true)
        )
        @Override
        public void create(@NotNull Context context) {
            try {
                Student current = retrieveStudent(context);

                PreparedStatement stmt = connection.prepareStatement("INSERT INTO \"student\" (\"name\", \"surname\", \"dob\", \"cdl\", \"aa\", \"gender\") VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);

                int i = 1;
                stmt.setString(i++, current.getName());
                stmt.setString(i++, current.getSurname());
                stmt.setTimestamp(i++, current.getDob());
                stmt.setString(i++, current.getCdl());
                stmt.setString(i++, current.getAa());
                stmt.setString(i, String.valueOf(current.getGender()));

                stmt.setQueryTimeout(30); // set timeout to 30 sec.
                int affectedRows = stmt.executeUpdate();

                if (affectedRows == 0) throw new InternalServerErrorResponse("Creating user failed, no rows affected.");

                try (ResultSet generateKeys = stmt.getGeneratedKeys()) {
                    if (generateKeys.next())
                        current.setStudentId(generateKeys.getLong(1));
                }

                this.getOne(context, String.valueOf(current.getStudentId()));
            } catch (JsonProcessingException e) {
                throw new InternalServerErrorResponse("XML: syntax error!");
            } catch (BadRequestResponse e) {
                throw new InternalServerErrorResponse("JSON: syntax error!");
            } catch (SQLException e) {
                throw new InternalServerErrorResponse("A SQL error occurred!");
            } catch (NullPointerException e) {
                throw new InternalServerErrorResponse("Content-Type not defined");
            }
        }

        private Student retrieveStudent(@NotNull Context context) throws JsonProcessingException {
            MediaType contentType = MediaType.parse(Objects.requireNonNull(context.contentType()));
            Student current;
            if (contentType.equals(MediaType.JSON_UTF_8)) current = context.bodyAsClass(Student.class);
            else if (contentType.equals(MediaType.XML_UTF_8))
                current = xmlMapper.readValue(context.body(), Student.class);
            else throw new InternalServerErrorResponse("Unknown format");
            return current;
        }

        @OpenApi(
                responses = {
                        @OpenApiResponse(
                                status = "200",
                                content = {
                                        @OpenApiContent(from = Student.class, type = "application/json;charset=UTF-8"),
                                        @OpenApiContent(from = Student.class, type = "text/xml;charset=UTF-8")
                                },
                                description = "Request successful")/*,
                        @OpenApiResponse(
                                status = "204", // alternative way
                                description = "Request successful with no content"
                        )*/
                }
        )
        @Override
        public void delete(@NotNull Context context, @NotNull String s) {
            try {
                this.getOne(context, s);
                PreparedStatement stmt = connection.prepareStatement("DELETE FROM \"student\" WHERE \"student_id\" = ?");
                stmt.setLong(1, Long.parseLong(s));
                stmt.setQueryTimeout(30); // set timeout to 30 sec.
                stmt.executeUpdate();
                // context.status(HttpStatus.NO_CONTENT_204);   // alternative way
            } catch (SQLException e) {
                throw new InternalServerErrorResponse("A SQL error occurred!");
            }
        }

        @OpenApi(
                responses = {
                        @OpenApiResponse(
                                status = "200",
                                content = {
                                        @OpenApiContent(from = Student.class, isArray = true, type = "application/json;charset=UTF-8"),
                                        @OpenApiContent(from = Student.class, isArray = true, type = "text/xml;charset=UTF-8")
                                },
                                description = "Request successful")
                },
                queryParams = {
                        @OpenApiParam(name = "na", description = "Name of a student"),
                        @OpenApiParam(name = "sur", description = "Surname of a student"),
                        @OpenApiParam(name = "dob", description = "Date of birth of a student", type = Timestamp.class),
                        @OpenApiParam(name = "cdl", description = "Corso di studi"),
                        @OpenApiParam(name = "aa", description = "Academic year"),
                        @OpenApiParam(name = "g", description = "Gender of the student", type = Character.class)
                }
        )
        @Override
        public void getAll(@NotNull Context context) {
            try {
                String query = "SELECT t.* FROM student t";
                List<String> values = new ArrayList<>(0);
                if (context.queryString() != null) {
                    StringJoiner joiner = new StringJoiner(" AND ", " WHERE ", "");
                    Map<String, String> paramColumnName = new HashMap<>();
                    paramColumnName.put("na", "name");
                    paramColumnName.put("sur", "surname");
                    paramColumnName.put("dob", "dob");
                    paramColumnName.put("cdl", "cdl");
                    paramColumnName.put("aa", "aa");
                    paramColumnName.put("g", "gender");
                    paramColumnName.forEach((key, value1) -> {
                        String value = context.queryParam(key);
                        if (value != null) {
                            joiner.add(value1 + " == ? COLLATE NOCASE");
                            values.add(value);
                        }
                    });
                    query += joiner.toString();
                }
                PreparedStatement stmt = connection.prepareStatement(query);
                List<Student> result = new ArrayList<>(0);
                if (context.queryString() != null)
                    for (int i = 0; i < values.size(); i++) stmt.setString(i + 1, values.get(i));
                stmt.setQueryTimeout(30); // set timeout to 30 sec.
                ResultSet rs = stmt.executeQuery();
                Student target;
                while (rs.next()) {
                    target = new Student();
                    loadStudent(rs, target);
                    result.add(target);
                }
                List<MediaType> mimes = getAcceptableMimes(context);
                if (mimes.contains(MediaType.JSON_UTF_8))
                    context.json(result).header("Content-Disposition", "inline; filename=\"students.json\"");
                else if (mimes.contains(MediaType.XML_UTF_8))
                    context.result(xmlMapper.writer().withRootName("Students").writeValueAsString(result))
                            .contentType(MediaType.XML_UTF_8.toString())
                            .header("Content-Disposition", "inline; filename=\"students.xml\"");
                else throw new InternalServerErrorResponse("Unknown format");
            } catch (SQLException e) {
                throw new InternalServerErrorResponse("A SQL error occurred!");
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        private List<MediaType> getAcceptableMimes(@NotNull Context context) {
            Enumeration<String> enumMines = context.req.getHeaders("Accept");
            List<MediaType> mimes = new ArrayList<>(0);
            while (enumMines.hasMoreElements()) {
                String[] inLineMime = enumMines.nextElement().split("\\s*,\\s*");
                for (String mime : inLineMime) mimes.add(MediaType.parse(mime));
            }
            return mimes;

        }

        private void loadStudent(ResultSet rs, Student target) throws SQLException {
            target.setStudentId(rs.getLong("student_id"));
            target.setName(rs.getString("name"));
            target.setSurname(rs.getString("surname"));
            target.setDob(rs.getTimestamp("dob"));
            target.setCdl(rs.getString("cdl"));
            target.setAa(rs.getString("aa"));
            target.setGender(rs.getString("gender").charAt(0));
        }

        @OpenApi(
                responses = {
                        @OpenApiResponse(
                                status = "200",
                                content = {
                                        @OpenApiContent(from = Student.class, type = "application/json;charset=UTF-8"),
                                        @OpenApiContent(from = Student.class, type = "text/xml;charset=UTF-8")
                                },
                                description = "Request successful")
                }
        )
        @Override
        public void getOne(@NotNull Context context, @NotNull String s) {
            try {
                PreparedStatement stmt = connection.prepareStatement("SELECT t.* FROM student t WHERE student_id = ?");
                stmt.setLong(1, Long.parseLong(s));
                Student target = new Student();
                stmt.setQueryTimeout(30); // set timeout to 30 sec.
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) loadStudent(rs, target);
                List<MediaType> mimes = getAcceptableMimes(context);
                if (mimes.contains(MediaType.JSON_UTF_8))
                    context.json(target).header("Content-Disposition", "inline; filename=\"students.json\"");
                else if (mimes.contains(MediaType.XML_UTF_8))
                    context.result(xmlMapper.writeValueAsString(target))
                            .contentType(MediaType.XML_UTF_8.toString())
                            .header("Content-Disposition", "inline; filename=\"students.xml\"");
                else throw new InternalServerErrorResponse("Unknown format");
            } catch (SQLException e) {
                throw new InternalServerErrorResponse("A SQL error occurred!");
            } catch (NumberFormatException e) {
                throw new InternalServerErrorResponse("Invalid path variable");
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        @OpenApi(
                responses = {
                        @OpenApiResponse(
                                status = "200",
                                content = {
                                        @OpenApiContent(from = Student.class, type = "application/json;charset=UTF-8"),
                                        @OpenApiContent(from = Student.class, type = "text/xml;charset=UTF-8")
                                },
                                description = "Request successful")
                },
                requestBody =
                @OpenApiRequestBody(content = {
                        @OpenApiContent(from = Student.class, type = "application/json;charset=UTF-8"),
                        @OpenApiContent(from = Student.class, type = "text/xml;charset=UTF-8")
                })
        )
        @Override
        public void update(@NotNull Context context, @NotNull String s) {
            try {
                Student current = retrieveStudent(context);

                StringJoiner joiner = new StringJoiner(", ", "UPDATE \"student\" SET ", " WHERE \"student_id\" = ?");
                for (Field field : Student.class.getDeclaredFields()) {
                    field.setAccessible(true);
                    if (!(field.getName().equals("studentId") || field.getName().equals("serialVersionUID")) && field.get(current) != null)
                        joiner.add("\"" + field.getName() + "\" = ?");
                }

                PreparedStatement stmt = connection.prepareStatement(joiner.toString());
                int i = 1;
                for (Field field : Student.class.getDeclaredFields()) {
                    field.setAccessible(true);
                    if (!(field.getName().equals("studentId") || field.getName().equals("serialVersionUID")) && field.get(current) != null)
                        stmt.setObject(i++, field.get(current));
                }
                stmt.setLong(i, Long.parseLong(s));

                stmt.setQueryTimeout(30); // set timeout to 30 sec.
                stmt.executeUpdate();
                this.getOne(context, s);
            } catch (JsonProcessingException e) {
                throw new InternalServerErrorResponse("XML: syntax error!");
            } catch (BadRequestResponse e) {
                throw new InternalServerErrorResponse("JSON: syntax error!");
            } catch (SQLException | IllegalAccessException e) {
                throw new InternalServerErrorResponse("A SQL error occurred!");
            } catch (NullPointerException e) {
                throw new InternalServerErrorResponse("Content-Type not defined");
            } catch (NumberFormatException e) {
                throw new InternalServerErrorResponse("Invalid path variable");
            }
        }
    }

}
