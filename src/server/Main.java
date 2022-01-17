package server;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Main {
    public static void main(String[] args) {
        new Server().bootstrap();
    }
}

class Server {
    private final static int BUFFER_SIZE = Integer.parseInt(getDataProperties("buffer.size"));
    private AsynchronousServerSocketChannel server;

    private final static String HEADERS = """
            HTTP/1.1 200 OK
            Server: first-http-server
            Content-Type: text/html
            Content-Length: %s
            Connection: close
                                              
            """;

    public void bootstrap() {
        try {
            server = AsynchronousServerSocketChannel.open();
            server.bind(new InetSocketAddress(getDataProperties("hostname"),
                    Integer.parseInt(getDataProperties("port"))));

            while (true) {
                Future<AsynchronousSocketChannel> accept = server.accept();
                handleClient(accept);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleClient(Future<AsynchronousSocketChannel> accept)
            throws InterruptedException, ExecutionException, TimeoutException, IOException {
        AsynchronousSocketChannel clientChannel = accept.get(60, TimeUnit.SECONDS);

        while (clientChannel != null && clientChannel.isOpen()) {
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
            StringBuilder stringBuilder = new StringBuilder();
            boolean keepReading = true;

            while (keepReading) {
                clientChannel.read(buffer).get();

                int position = buffer.position();
                keepReading = position == BUFFER_SIZE;

                byte[] array = keepReading ? buffer.array() : Arrays.copyOfRange(buffer.array(), 0, position);

                stringBuilder.append(new String(array));
                buffer.clear();
            }
            String body = "<html><body><h1>Hello</h1></body></html>";
            String page = String.format(HEADERS, body.length()) + body;
            ByteBuffer response = ByteBuffer.wrap(page.getBytes());
            clientChannel.write(response);
            clientChannel.close();
        }
    }

    private static String getDataProperties(String param) {
        Properties props = new Properties();

        try {
            props.load(new InputStreamReader(
                    new FileInputStream("src/server/config.properties"), StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (props.getProperty(param) == null) {
            throw new NullPointerException("The " + param + " parameter is not in the file!");
        }
        return props.getProperty(param);
    }
}
