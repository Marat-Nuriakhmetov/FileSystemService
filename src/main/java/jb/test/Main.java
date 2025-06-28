package jb.test;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

// Main application
public class Main {
    public static void main(String[] args) throws Exception {
        // Create service instance
        FileService fileService = new FileServiceImpl();

        // Create and configure Jetty server
        Server server = new Server(8080);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // Add JSON-RPC servlet
        context.addServlet(new ServletHolder(new JsonRpcServlet(fileService)), "/jsonrpc");

        // Start server
        server.start();
        server.join();
    }
}
