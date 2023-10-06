import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Time;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class tcpServer {
    static int port;
    static int REPORT_INTERVAL = 3000;
    static final int THREAD_POOL_SIZE = 10;
    static String UP_DIR = "uploads/";
    static int BUF_SIZE = 4096;
    private static ConcurrentHashMap<Socket, Long> clientsHashMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        port = Integer.parseInt(args[0]);
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName("192.168.3.46");
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        try (ServerSocket serverSocket = new ServerSocket(port, 5, inetAddress)) {
            System.out.println("Server started!");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                Runnable clientHandler = new ClientHandler(clientSocket);
                executorService.execute(clientHandler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (
                    InputStream inputStream = clientSocket.getInputStream();
                    DataInputStream dataInputStream = new DataInputStream(inputStream);
                    OutputStream outputStream = clientSocket.getOutputStream();
                    DataOutputStream dataOutputStream = new DataOutputStream(outputStream)
            ) {
                String filename = dataInputStream.readUTF();
                long fileSize = dataInputStream.readLong();

                System.out.println(filename + " " + fileSize + " bytes");

                File directory = new File(UP_DIR);
                if (!directory.exists()) {
                    directory.mkdir();
                }

                File file = new File(UP_DIR + filename);
                try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                    byte[] buffer = new byte[BUF_SIZE];
                    int bytesRead;
                    long totalBytesReceived = 0;

                    long startTime = System.currentTimeMillis();
                    clientsHashMap.put(clientSocket, startTime);

                    long instantSpeed = 0, avgSpeed = 0, currentTime = 0;
                    double elapsedTime = 0;

                    while ((bytesRead = dataInputStream.read(buffer)) != -1) {
                        fileOutputStream.write(buffer, 0, bytesRead);
                        totalBytesReceived += bytesRead;

                        currentTime = System.currentTimeMillis();
                        elapsedTime = currentTime - startTime;
                        if (elapsedTime < 1) {
                            elapsedTime = 1;
                        }

                        if (elapsedTime >= REPORT_INTERVAL) {
                            instantSpeed = (long) (totalBytesReceived / elapsedTime * 1000);
                            avgSpeed = (long) (totalBytesReceived / (elapsedTime / 1000));

                            System.out.println("Instant speed: " + instantSpeed + " byte/sec");
                            System.out.println("Average speed: " + avgSpeed + " byte/sec");
                            System.out.println();

                            startTime = currentTime;
                            totalBytesReceived = 0;
                        }

                    }

                    currentTime = System.currentTimeMillis();
                    elapsedTime = currentTime - startTime;
                    instantSpeed = (long) (totalBytesReceived / elapsedTime * 1000);
                    avgSpeed = (long) (totalBytesReceived / (elapsedTime / 1000));

                    System.out.println("Instant speed: " + instantSpeed + " byte/sec");
                    System.out.println("Average speed: " + avgSpeed + " byte/sec");

                    System.out.println(filename + " received");

                    dataInputStream.close();
                    dataOutputStream.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                clientsHashMap.remove(clientSocket);
            }
        }
    }
}