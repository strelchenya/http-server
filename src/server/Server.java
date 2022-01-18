package server;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

class Server {
    private final static String pathProperties = "src/server/config.properties";
    private final static int BUFFER_SIZE = Integer.parseInt(getDataProperties("buffer.size"));
    private final HttpHandler handler;
    private AsynchronousServerSocketChannel server;

    public Server(HttpHandler handler) {
        this.handler = handler;
    }

    private static String getDataProperties(String param) {
        Properties props = new Properties();

        try {
            props.load(new InputStreamReader(
                    new FileInputStream(pathProperties), StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (props.getProperty(param) == null) {
            throw new NullPointerException("The " + param + " parameter is not in the file!");
        }
        return props.getProperty(param);
    }

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
            throws InterruptedException, ExecutionException, IOException {
        AsynchronousSocketChannel clientChannel = accept.get();

        while (clientChannel != null && clientChannel.isOpen()) {
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
            StringBuilder stringBuilder = new StringBuilder();
            boolean keepReading = true;

            while (keepReading) {
                int readResult = clientChannel.read(buffer).get();
                keepReading = readResult == BUFFER_SIZE;
                buffer.flip();
                CharBuffer charBuffer = StandardCharsets.UTF_8.decode(buffer);
                stringBuilder.append(charBuffer);
                buffer.clear();
            }

            HttpRequest request = new HttpRequest(stringBuilder.toString());
            HttpResponse response = new HttpResponse();

            if (handler != null) {
                try {
                    String body = this.handler.handle(request, response);

                    if (body != null && !body.isBlank()) {
                        if (response.getHeaders().get("Content-Type") == null) {
                            response.addHeader("Content-Type", "text/html; charset=utf-8");
                        }
                        response.setBody(body);
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    response.setStatusCode(500);
                    response.setStatus("Internal server error");
                    response.addHeader("Content-Type", "text/html; charset=utf-8");
                    response.setBody("<html><body><h1>Error happens</h1></body></html>");
                }
            } else {
                response.setStatusCode(404);
                response.setStatus("Not Found");
                response.addHeader("Content-Type", "text/html; charset=utf-8");
                response.setBody("<html><body><h1>Resource not found</h1></body></html>");
            }

            ByteBuffer resp = ByteBuffer.wrap(response.getBytes());
            clientChannel.write(resp);
            clientChannel.close();
        }
    }
}
