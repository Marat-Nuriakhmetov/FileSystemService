package jb.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.arteam.simplejsonrpc.server.JsonRpcServer;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.stream.Collectors;

// JSON-RPC servlet
public class JsonRpcServlet extends HttpServlet {
    private final JsonRpcServer jsonRpcServer;
    private final ObjectMapper mapper;
    private final Object service;

    public JsonRpcServlet(Object service) {
        this.jsonRpcServer = new JsonRpcServer();
        this.mapper = new ObjectMapper();
        this.service = service;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        // Set response content type
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        // Handle JSON-RPC request
        String textRequest = req.getReader().lines().collect(Collectors.joining());
        String result = jsonRpcServer.handle(textRequest, service);
        resp.getWriter().write(result);
    }
}

