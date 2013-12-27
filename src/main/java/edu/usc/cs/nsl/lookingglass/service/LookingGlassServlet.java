package edu.usc.cs.nsl.lookingglass.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 *
 * @author matt
 */
public class LookingGlassServlet extends HttpServlet {

    private JsonRpcServer rpcServer;
    private TracerouteService tracerouteService;
    
    public LookingGlassServlet(TracerouteService tracerouteService) {
        this.tracerouteService = tracerouteService;
    }
    
    @Override
    public void init() {
        rpcServer = new JsonRpcServer(new ObjectMapper(), tracerouteService);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        rpcServer.handle(req, resp);
    }
}
