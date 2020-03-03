package it.uniupo.util;

import java.io.*;

/**
 * A class to convert an obj to byte[] and vice versa.
 *
 * @author Lorenzo Ferron
 * @version 2020.02.20
 */
public class ByteUtils {

    public static byte[] objToByte(Object obj) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream objStream = new ObjectOutputStream(byteStream);
        objStream.writeObject(obj);

        return byteStream.toByteArray();
    }

    public static Object byteToObj(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
        ObjectInputStream objStream = new ObjectInputStream(byteStream);

        return objStream.readObject();
    }

}
