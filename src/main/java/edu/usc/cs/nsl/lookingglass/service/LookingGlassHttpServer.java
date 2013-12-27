package edu.usc.cs.nsl.lookingglass.service;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 *
 * @author matt
 */
public class LookingGlassHttpServer {
    
    private static Logger log = Logger.getLogger(LookingGlassHttpServer.class);
    
    private Server server;
    private int port;
    private TracerouteService tracerouteService;
    private QueryProcessor queryProcessor;
    
    public LookingGlassHttpServer(int port, TracerouteService tracerouteService, QueryProcessor queryProcessor){
        this.port = port;
        this.tracerouteService = tracerouteService;
        this.queryProcessor = queryProcessor;
    }
    
    public void start() throws Exception {
        log.info("Starting the HTTP server");
        
        server = new Server(port);
        
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        
        LookingGlassServlet servlet = new LookingGlassServlet(tracerouteService);
        ServletHolder servletHolder = new ServletHolder(servlet);
        context.addServlet(servletHolder, "/lg");
        
        /**
         * Start the query processor in a new thread
         */
        new Thread(){
            @Override
            public void run() {
                try {
                    queryProcessor.run();
                } catch(Exception ex){
                    log.error("There was an error that caused the query processor to exit", ex);
                }
            }
        }.start();
        
        server.start();
        server.join();
    }
    
    public void stop() throws Exception {
        log.info("Shutting the server down");
        
        queryProcessor.shutdown();
        server.stop();
    }
}
