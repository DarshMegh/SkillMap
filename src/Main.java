package skillmap;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        HttpServer server = new HttpServer(port);
        server.start();
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║       SkillMap AI — Server Up!       ║");
        System.out.println("║   http://localhost:" + port + "              ║");
        System.out.println("╚══════════════════════════════════════╝");
    }
}