import java.io.*;

public class DeepCopy {
    public static Board deepCopy(Board object) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);

        objectOutputStream.writeObject(object);

        objectOutputStream.close();
        outputStream.close();

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

        Board copy = (Board) objectInputStream.readObject();

        objectInputStream.close();
        inputStream.close();

        return copy;
    }
}
