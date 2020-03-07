package rpc.common;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import it.uniupo.beans.Student;
import org.apache.commons.lang3.SystemUtils;
import rpc.JsonService;
import rpc.common.util.InputUtils;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.logging.Logger;

/**
 * A pleasant human interface.
 *
 * @author Lorenzo Ferron
 * @version 2020.02.26
 */
public class GUI {

    private static final Logger LOGGER = Logger.getLogger(GUI.class.getName());
    private final JsonService service;

    public GUI(JsonService service) {
        this.service = service;
    }

    public void start() {
        clearScreen();
        displayLogo();
        displayMenu();
    }

    private void displayMenu() {
        showMenu();
        selectAction(choose(JsonService.class.getDeclaredMethods().length + 2, false));
    }

    private int choose(int end, boolean subMenu) {
        int choice = -1;
        do {
            try {
                String subExitQuery = subMenu ? ", 0 to quit" : "";
                choice = InputUtils.readInt("Enter a number [1-" + end + subExitQuery + "]: ");
                if (choice < 0 || choice > end || !subMenu && choice == 0) {
                    LOGGER.info("Number is not valid [1-" + end + subExitQuery + "]\n");
                    System.out.print("[Re-]");
                }
            } catch (NumberFormatException e) {
                LOGGER.warning("Invalid input, this is not a number.\n");
                System.out.print("[Re-]");
            }
        } while (choice < 0 || choice > end || !subMenu && choice == 0);
        return choice;
    }

    private void selectAction(int choose) {
        switch (choose) {
            case 1:
                searchAction();
                break;
            case 2:
                createAction();
                break;
            case 3:
                deleteAction();
                break;
            case 4:
                getOneAction();
                break;
            case 5:
                updateAction();
                break;
            case 6:
                multithreadingDemo();
                break;
            case 7:
                InputUtils.close();
                return;
            default:
                break;
        }
        displayMenu();
    }

    private void multithreadingDemo(){
        clearScreen();
        System.out.println("Main Menu > Multithreading Demo");
        System.out.println();

        final CyclicBarrier gate = new CyclicBarrier(3);
        Thread t1 = new Thread(() -> {
            Gson gson = new Gson();
            try {
                gate.await();
                List<Student> students = gson.fromJson(service.getAll(new HashMap<>()), new TypeToken<List<Student>>() {
                }.getType());
                printSearchResults(students);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Thread t2 = new Thread(() -> {
            Student student = new Student();
            student.setAa("2019-2020");
            student.setCdl("Informatica");
            student.setDob(new Timestamp(896018400000L));
            student.setGender('F');
            student.setName("Valentina");
            student.setSurname("Ferron");
            try {
                gate.await();
                System.out.println();
                System.out.println(service.create(student));
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println();
        });
        t1.start();
        t2.start();
        try {
            gate.await();
            System.out.println("all threads started");
            t1.join();
            t2.join();
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
    }

    private void updateAction() {
        clearScreen();
        System.out.println("Main Menu > Update a student info");
        System.out.println();
        Long studentId;
        try {
            studentId = InputUtils.readLong("Insert student ID: ");
        } catch (NumberFormatException e) {
            LOGGER.warning("Invalid input, ID is not a number.\n");
            return;
        }
        try {
            Student oldStudent = service.getOne(studentId);
            if (oldStudent.getStudentId() == null) System.out.println("No results found.");
            else {
                Student updatedStudent = readStudent(oldStudent, false);
                System.out.println();
                if (updatedStudent.equals(oldStudent)) System.out.println("Nothing to do!");
                else System.out.println(service.update(studentId, updatedStudent));
            }
            System.out.println();
        } catch (StringIndexOutOfBoundsException e) {
            LOGGER.warning("Gender is empty!");
        } catch (NumberFormatException e) {
            LOGGER.warning("Invalid input, Date of Birthday is not a number.\n");
        } catch (Exception e) {
            LOGGER.warning(e.getMessage() + "\n");
        }
    }

    private void getOneAction() {
        clearScreen();
        System.out.println("Main Menu > Find a student");
        System.out.println();
        try {
            Long studentId = InputUtils.readLong("Insert student ID: ");
            Student student = service.getOne(studentId);
            System.out.println();
            if (student.getStudentId() == null) System.out.println("No results found");
            else System.out.println(student);
            System.out.println();
        } catch (NumberFormatException e) {
            LOGGER.warning("Invalid input, ID is not a number.\n");
        } catch (Exception e) {
            LOGGER.warning(e.getMessage() + "\n");
        }
    }

    private void deleteAction() {
        clearScreen();
        System.out.println("Main Menu > Unenroll a student");
        System.out.println();
        try {
            Long studentId = InputUtils.readLong("Insert student ID: ");
            Student student = service.delete(studentId);
            System.out.println();
            if (student.getStudentId() == null) System.out.println("No deleting");
            else System.out.println(student);
            System.out.println();
        } catch (NumberFormatException e) {
            LOGGER.warning("Invalid input, ID is not a number.\n");
        } catch (Exception e) {
            LOGGER.warning(e.getMessage() + "\n");
        }
    }

    private void printSearchResults(List<Student> students) {
        System.out.println("\nSEARCH RESULTS (" + students.size() + "): \n");
        if (students.isEmpty()) System.out.println("No results found\n");
        else students.forEach(res -> {
            System.out.println(res);
            System.out.println();
        });
    }

    private void searchAction() {
        clearScreen();
        System.out.println("Main Menu > Search an existing student for...");
        try {
            Student student = readStudent(true);
            Map<String, String> filters = new HashMap<>();
            if (student.getSurname() != null && !student.getSurname().isEmpty())
                filters.put("sur", student.getSurname());
            if (student.getName() != null && !student.getName().isEmpty())
                filters.put("na", student.getName());
            if (student.getAa() != null && !student.getAa().isEmpty())
                filters.put("aa", student.getAa());
            if (student.getDob() != null)
                filters.put("dob", String.valueOf(student.getDob().getTime()));
            if (student.getCdl() != null && !student.getCdl().isEmpty())
                filters.put("cdl", student.getCdl());
            if (student.getGender() != null)
                filters.put("g", String.valueOf(student.getGender()));
            Gson gson = new Gson();
            List<Student> students = gson.fromJson(service.getAll(filters), new TypeToken<List<Student>>() {
            }.getType());
            printSearchResults(students);
        } catch (StringIndexOutOfBoundsException e) {
            LOGGER.warning("Gender is empty!");
        } catch (NumberFormatException e) {
            LOGGER.warning("Invalid input, Date of Birthday is not a number.\n");
        } catch (Exception e) {
            LOGGER.warning(e.getMessage() + "\n");
        }
    }

    private Student readStudent(boolean search) {
        return readStudent(null, search);
    }

    private Student readStudent(Student student, boolean search) {
        System.out.println();
        if (!search) System.out.println("* = required\n");

        String name = InputUtils.readString("Name" + (search ? "" : "*") + (student == null ? "" : " [" + student.getName() + "]") + ": ");
        if (name.isEmpty() && !search)
            if (student == null) throw new IllegalArgumentException("Name is empty!");
            else name = student.getName();

        String surname = InputUtils.readString("Surname" + (search ? "" : "*") + (student == null ? "" : " [" + student.getSurname() + "]") + ": ");
        if (surname.isEmpty() && !search)
            if (student == null) throw new IllegalArgumentException("Surname is empty!");
            else surname = student.getSurname();

        String dobStr = InputUtils.readString("Date of Birthday" + (search ? "" : "*") + (student == null ? "" : " [" + student.getDob().getTime() + "]") + ": ");
        Timestamp dob = null;
        if (dobStr.isEmpty()) {
            if (!search)
                if (student == null) throw new IllegalArgumentException("Date Of Birthday is empty!");
                else dob = student.getDob();
        } else dob = new Timestamp(Long.parseLong(dobStr));

        Character gender;
        try {
            gender = Character.toUpperCase(InputUtils.readChar("Gender" + (search ? "" : "*") + (student == null ? "" : " [" + student.getGender() + "]") + ": "));
            if (gender != 'M' && gender != 'F')
                throw new IllegalArgumentException("Invalid input, Gender is not M(ale) or F(emale).");
        } catch (StringIndexOutOfBoundsException e) {
            if (!search)
                if (student == null) throw e;
                else gender = student.getGender();
            else gender = null;
        }

        String cdl = InputUtils.readString("Corso di Laurea" + (search ? "" : "*") + (student == null ? "" : " [" + student.getCdl() + "]") + ": ");
        if (cdl.isEmpty() && !search)
            if (student == null) throw new IllegalArgumentException("Corso di Laurea is empty!");
            else cdl = student.getCdl();

        String aa = InputUtils.readString("Anno Accademico" + (search ? "" : "*") + " (YYYY-YYYY)" + (student == null ? "" : " [" + student.getAa() + "]") + ": ");
        if (aa.isEmpty()) {
            if (!search)
                if (student == null) throw new IllegalArgumentException("Anno Accademico is empty!");
                else aa = student.getAa();
        } else if (!aa.matches("[0-9]{4}-[0-9]{4}"))
            throw new IllegalArgumentException("Invalid input, Anno Accademico does not match format pattern.");
        else {
            String[] years = aa.split("-");
            if (Integer.parseInt(years[0]) + 1 != Integer.parseInt(years[1]))
                throw new IllegalArgumentException("Invalid input, Anno Accademico does not match format pattern.");
        }

        Student newStudent = new Student();
        if (student != null && !search) newStudent.setStudentId(student.getStudentId());
        newStudent.setName(name);
        newStudent.setSurname(surname);
        newStudent.setGender(gender);
        newStudent.setCdl(cdl);
        newStudent.setAa(aa);
        newStudent.setDob(dob);
        return newStudent;
    }

    private void createAction() {
        clearScreen();
        System.out.println("Main Menu > Enroll a new student");
        try {
            Student student = readStudent(false);
            System.out.println();
            System.out.println(service.create(student));
            System.out.println();
        } catch (StringIndexOutOfBoundsException e) {
            LOGGER.warning("Gender is empty!");
        } catch (NumberFormatException e) {
            LOGGER.warning("Invalid input, Date of Birthday is not a number.\n");
        } catch (Exception e) {
            LOGGER.warning(e.getMessage() + "\n");
        }
    }

    /**
     * This method clear the current screen
     *
     * <p>
     * On Windows systems, ANSI escape code are supported by PowerShell 6.0 and later.
     * </p>
     */
    private void clearScreen() {
        System.out.println(SystemUtils.IS_OS_WINDOWS ? "e[H`e[2J" : "\033[H\033[2J");
        System.out.flush();
    }

    // ======================================================
    // (START) ASCII ART METHODS
    // ======================================================

    private void showMenu() {
        System.out.print(
                "+--------------------------------------------------+\n" +
                        "|                                                  |\n" +
                        "| Options:                                         |\n" +
                        "|                                                  |\n" +
                        "|        1. Search an existing student for...      |\n" +
                        "|        2. Enroll a new student                   |\n" +
                        "|        3. Unenroll a student                     |\n" +
                        "|        4. Find a student                         |\n" +
                        "|        5. Update a student info                  |\n" +
                        "|        6. Multithreading Demo                    |\n" +
                        "|                                                  |\n" +
                        "|        7. QUIT                                   |\n" +
                        "|                                                  |\n" +
                        "+--------------------------------------------------+\n");
        System.out.println();
    }

    private void displayLogo() {
        System.out.print(
                "+--------------------------------------------------+\n" +
                        "|                                                  |\n" +
                        "|                                                  |\n" +
                        "|               ____                               |\n" +
                        "|              / __ \\___  ____ ___  ____           |\n" +
                        "|             / / / / _ \\/ __ `__ \\/ __ \\          |\n" +
                        "|            / /_/ /  __/ / / / / / /_/ /          |\n" +
                        "|           /_____/\\___/_/ /_/ /_/\\____/           |\n" +
                        "|                                                  |\n" +
                        "|           by Lorenzo Ferron (20024182)           |\n" +
                        "|                                                  |\n");
    }

    // ======================================================
    // (END) ASCII ART METHODS
    // ======================================================

}
