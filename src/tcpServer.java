import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class tcpServer {
    static int port;
    static int REPORT_INTERVAL = 3000;
    static final int THREAD_POOL_SIZE = 10;
    static String UP_DIR = "uploads/";
    private static ConcurrentHashMap<Socket, Long> clientStartTimes = new ConcurrentHashMap<>();


    public static void main(String[] args) {
        port = Integer.parseInt(args[0]);
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
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

                File directory = new File(UP_DIR);
                if (!directory.exists()) {
                    directory.mkdir();
                }

                File file = new File(UP_DIR + filename);
                try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    long totalBytesReceived = 0;

                    long startTime = System.currentTimeMillis();
                    clientStartTimes.put(clientSocket, startTime);

                    while ((bytesRead = dataInputStream.read(buffer)) != -1) {
                        fileOutputStream.write(buffer, 0, bytesRead);
                        totalBytesReceived += bytesRead;

                        long currentTime = System.currentTimeMillis();
                        long elapsedTime = currentTime - startTime;

                        if (elapsedTime >= REPORT_INTERVAL) {
                            double instantSpeed = (double) totalBytesReceived / elapsedTime * 1000;
                            double avgSpeed = (double) totalBytesReceived / (elapsedTime / 1000.0);

                            System.out.println("Instant speed: " + instantSpeed + " byte/sec");
                            System.out.println("Average speed: " + avgSpeed + " byte/sec");

                            startTime = currentTime;
                            totalBytesReceived = 0;
                        }
                    }

                    dataOutputStream.writeBoolean(fileSize == file.length());

                    System.out.println("File " + filename + " transferred.");
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

                clientStartTimes.remove(clientSocket);
            }
        }
    }
}