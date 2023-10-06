import org.w3c.dom.ls.LSOutput;

import java.io.*;
import java.net.Socket;
import java.nio.file.Paths;

public class tcpClient {
    static String path;
    static String ip;
    static int port;
    static long fileSize;

    public static void main(String[] args) {
        path = args[0];
        ip = args[1];
        port = Integer.parseInt(args[2]);
        File file = new File(path);
        if (file.exists()) {
            fileSize = file.length();
            System.out.println("File size: " + fileSize);
        } else {
            System.out.println("File doesn't exists");
        }

        try (
                Socket socket = new Socket(ip, port);
                OutputStream outputStream = socket.getOutputStream();
                DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
                InputStream inputStream = new FileInputStream(path);
                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
        ) {
            dataOutputStream.writeUTF(Paths.get(path).getFileName().toString());
            dataOutputStream.writeLong(fileSize);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                dataOutputStream.write(buffer, 0, bytesRead);
            }

            System.out.println();
            System.out.println("File " + Paths.get(path).getFileName().toString() + " received");

            dataOutputStream.close();
            dataInputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}