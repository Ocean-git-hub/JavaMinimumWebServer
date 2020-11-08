import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.*;

public class Main {
    private static final int PORT = 80;
    private static final String SERVER_NAME = "Java-Webserver";
    private static String documentRoot;

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("usage: <document root> <back log que> <threads>");
            System.exit(1);
        }

        documentRoot = args[0];
        ExecutorService threadPool = Executors.newFixedThreadPool(Integer.parseInt(args[2]));
        try {
            ServerSocket serverSocket = new ServerSocket(PORT, Integer.parseInt(args[1]));
            System.out.println("Startup " + SERVER_NAME);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(() -> exchangeConnection(clientSocket));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void exchangeConnection(Socket socket) {
        try {
            String[] requestParse = new BufferedReader(new InputStreamReader(socket.getInputStream())).
                    readLine().split(" ");

            if (requestParse.length != 3 || !requestParse[2].startsWith("HTTP/")) {
                socket.close();
                return;
            }

            boolean isGet;
            if (!((isGet = requestParse[0].equals("GET")) || requestParse[0].equals("HEAD"))) {
                socket.close();
                return;
            }

            if (requestParse[1].endsWith("/"))
                requestParse[1] += "index.html";
            OutputStream socketOutputStream = socket.getOutputStream();
            Path filePath = Paths.get(documentRoot + requestParse[1]);
            if (Files.isReadable(filePath)) {
                socketOutputStream.
                        write(("HTTP/1.0 200 OK\r\n" +
                                "Server: " + SERVER_NAME + "\r\n\r\n").getBytes());
                if (isGet)
                    socketOutputStream.write(Files.readAllBytes(filePath));
            } else
                socketOutputStream.
                        write(("HTTP/1.0 404 Not Found\r\n" +
                                "Server: " + SERVER_NAME + "\r\n\r\n" +
                                "<html><head><title>404 Not Found</title></head>" +
                                "<body>404 Not Found</body></html>").getBytes());
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
