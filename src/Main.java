import java.io.*;
import java.net.*;
import java.nio.file.*;

public class Main {
    private static final int PORT = 8080;
    private static final String SERVER_NAME = "Java-Webserver";
    private static String documentRoot;

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("usage: <document root> <back log que> <threads>");
            System.exit(1);
        }

        documentRoot = args[0];
        try {
            ServerSocket serverSocket = new ServerSocket(PORT, Integer.parseInt(args[1]));
            System.out.println("Startup " + SERVER_NAME);

            int threads = Integer.parseInt(args[2]);
            for (int i = 0; i < threads; i++)
                new Thread(() -> thread_doing(serverSocket)).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void thread_doing(ServerSocket serverSocket) {
        while (true) {
            try {
                exchangeConnection(serverSocket.accept());
            } catch (IOException e) {
                e.printStackTrace();
            }
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
                                "Server: " + SERVER_NAME + "\r\n" +
                                "Connection: close\r\n" +
                                "Content-Length: " + Files.size(filePath) + "\r\n\r\n").getBytes());
                if (isGet)
                    socketOutputStream.write(Files.readAllBytes(filePath));
            } else
                socketOutputStream.
                        write(("HTTP/1.0 404 Not Found\r\n" +
                                "Server: " + SERVER_NAME + "\r\n" +
                                "Connection: close\r\n" +
                                "Content-Length: 80" + "\r\n\r\n" +
                                "<html><head><title>404 Not Found</title></head>" +
                                "<body>404 Not Found</body></html>").getBytes());
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
