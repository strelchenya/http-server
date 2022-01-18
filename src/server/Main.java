package server;

public class Main {
    public static void main(String[] args) {
        new Server((request, response) -> "<html><body><h1>Hello</h1>It's handler</body></html>").bootstrap();
    }
}

