package rpc.common.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Vector;

/**
 * Una semplice classe per leggere stringhe e numeri
 * dallo standard input.
 */

public final class InputUtils {

    private static BufferedReader reader =
            new BufferedReader(new InputStreamReader(System.in));

    private InputUtils() {
        // Empty body...
    }

    /**
     * Legge una linea di input. Nell'improbabile caso di una
     * IOException, il programma termina.
     *
     * @return restituisce la linea di input che l'utente ha battuto.
     */
    public static String readString() {
        String inputLine = "";
        try {
            inputLine = reader.readLine();
        } catch (IOException e) {
            e.getStackTrace();
            System.exit(1);
        }
        return inputLine;
    }

    public static String readString(String msg) {
        System.out.print(msg);
        return readString();
    }

    /**
     * Legge una linea di input e la converte in un byte.
     * Eventuali spazi bianchi prima e dopo l'intero vengono ignorati.
     *
     * @return l'intero dato in input dall'utente
     */
    public static byte readByte() {
        String inputString = readString();
        inputString = inputString.trim();
        return Byte.parseByte(inputString);
    }

    public static byte readByte(String msg) {
        System.out.print(msg);
        return readByte();
    }

    /**
     * Legge una linea di input e la converte in uno short.
     * Eventuali spazi bianchi prima e dopo l'intero vengono ignorati.
     *
     * @return l'intero dato in input dall'utente
     */
    public static short readShort() {
        String inputString = readString();
        inputString = inputString.trim();
        return Short.parseShort(inputString);
    }

    public static short readShort(String msg) {
        System.out.print(msg);
        return readShort();
    }

    /**
     * Legge una linea di input e la converte in un int.
     * Eventuali spazi bianchi prima e dopo l'intero vengono ignorati.
     *
     * @return l'intero dato in input dall'utente
     */
    public static int readInt() {
        String inputString = readString();
        inputString = inputString.trim();
        return Integer.parseInt(inputString);
    }

    public static int readInt(String msg) {
        System.out.print(msg);
        return readInt();
    }

    /**
     * Legge una linea di input e la converte in un long.
     * Eventuali spazi bianchi prima e dopo l'intero vengono ignorati.
     *
     * @return l'intero dato in input dall'utente
     */
    public static long readLong() {
        String inputString = readString();
        inputString = inputString.trim();
        return Long.parseLong(inputString);
    }

    public static long readLong(String msg) {
        System.out.print(msg);
        return readLong();
    }

    /**
     * Legge una linea di input e la converte in un numero
     * in virgola mobile a precisione singola.  Eventuali spazi bianchi prima e
     * dopo il numero vengono ignorati.
     *
     * @return il numero dato in input dall'utente
     */
    public static float readFloat() {
        String inputString = readString();
        inputString = inputString.trim();
        return Float.parseFloat(inputString);
    }

    public static float readFloat(String msg) {
        System.out.print(msg);
        return readFloat();
    }


    /**
     * Legge una linea di input e la converte in un numero
     * in virgola mobile a precisione doppia.  Eventuali spazi bianchi prima e
     * dopo il numero vengono ignorati.
     *
     * @return il numero dato in input dall'utente
     */
    public static double readDouble() {
        String inputString = readString();
        inputString = inputString.trim();
        return Double.parseDouble(inputString);
    }

    public static double readDouble(String msg) {
        System.out.print(msg);
        return readDouble();
    }


    /**
     * Legge una linea di input e ne estrae il primo carattere.
     *
     * @return il primo carattere della riga data in input dall'utente
     */
    public static char readChar() {
        String inputString = readString();
        return inputString.charAt(0);
    }

    public static char readChar(String msg) {
        System.out.print(msg);
        return readChar();
    }

    /**
     * Legge una linea di input e restituisce true se la stringa
     * e' equals a "true" a meno di maiuscole e minuscole, false altrimenti.
     *
     * @return il booeano dato in input dall'utente
     */
    public static boolean readBool() {
        String inputString = readString();
        inputString = inputString.trim();
        return Boolean.parseBoolean(inputString);
    }

    public static boolean readBool(String msg) {
        System.out.print(msg);
        return readBool();
    }


    /**
     * Legge una sequenza di stringhe conclusa dalla stringa vuota e
     * restituisce la sequenza in un nuovo array di stringhe.
     *
     * @return l'array delle stringhe date in input dal'utente
     */
    public static String[] readSeq() {
        return readSeq("");
    }

    public static String[] readSeq(String prompt) {
        List<String> seqTemp = new Vector<>();
        System.out.print(prompt);
        String inputString = readString();
        while (inputString.length() > 0) {
            seqTemp.add(inputString);
            System.out.print(prompt);
            inputString = readString();
        }
        String[] seq = new String[seqTemp.size()];
        return seqTemp.toArray(seq);
    }

    public static String[] readSeq(String msg, String prompt) {
        System.out.println(msg);
        return readSeq(prompt);
    }

    /**
     * Chiude il BufferedReader evitando memory leak. Da chiamare prima di terminare.
     */
    public static void close() {
        try {
            reader.close();
        } catch (IOException ignored) {
        }
    }
}
