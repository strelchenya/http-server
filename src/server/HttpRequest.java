package server;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
    private final static String DELIMITER = "\r\n\r\n";
    private final static String NEW_LINE = "\r\n";
    private final static String HEAD_DELIMITER = ":";

    private final HttpMethod method;
    private final String url;
    private final Map<String, String> headers;
    private final String body;

    private final String clientMessage;

    public HttpRequest(String clientMessage) {
        this.clientMessage = clientMessage;
        String[] parts = clientMessage.split(DELIMITER);
        String head = parts[0];
        String[] headers = head.split(NEW_LINE);
        String[] firstLine = headers[0].split(" ");
        method = HttpMethod.valueOf(firstLine[0]);
        url = firstLine[1];

        this.headers = Collections.unmodifiableMap(
                new HashMap<>() {{
                    for (int i = 1; i < headers.length; i++) {
                        String[] headerPart = headers[i].split(HEAD_DELIMITER, 2);
                        put(headerPart[0].trim(), headerPart[1].trim());
                    }
                }}
        );

        String bodyLength = this.headers.get("Content-Length");
        int length = bodyLength != null ? Integer.parseInt(bodyLength) : 0;
        this.body = parts.length > 1 ? parts[1].trim().substring(0, length) : "";
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public String getClientMessage() {
        return clientMessage;
    }
}
