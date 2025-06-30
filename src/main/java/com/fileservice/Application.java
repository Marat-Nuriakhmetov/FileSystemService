package com.fileservice;

import com.fileservice.config.AppModule;
import com.fileservice.controller.FileServiceController;
import com.fileservice.controller.JsonRpcServlet;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

// Main application
public class Application {
    public static void main(String[] args) throws Exception {
        // Create Guice injector with AppModule
        Injector injector = Guice.createInjector(new AppModule());

        // Get FileServiceController instance from injector
        FileServiceController fileServiceController = injector.getInstance(FileServiceController.class);

        // TODO add root folder via env var or args

        // Create and configure Jetty server
        Server server = new Server(8080);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // TODO improve error handling
        server.setErrorHandler(new ErrorHandler());

        // Add JSON-RPC servlet
        context.addServlet(new ServletHolder(new JsonRpcServlet(fileServiceController)), "/fos");

        // Start server
        server.start();
        server.join();
    }
}
