package edu.usc.cs.nsl.lookingglass.service;

import java.util.Map;
import org.apache.log4j.Logger;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Credential;

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
    private Map<String,String> authKeys;
    
    /**
     * 
     * @param port
     * @param tracerouteService
     * @param queryProcessor
     * @param authKeys 
     */
    public LookingGlassHttpServer(int port, TracerouteService tracerouteService, 
            QueryProcessor queryProcessor, Map<String,String> authKeys){
        this.port = port;
        this.tracerouteService = tracerouteService;
        this.queryProcessor = queryProcessor;
        this.authKeys = authKeys;
    }
    
    public void start() throws Exception {
        log.info("Starting the HTTP server");
        
        server = new Server(port);
        
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        
        SecurityHandler securityHandler = configureSecurityHandler();
        context.setSecurityHandler(securityHandler);
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
    
    /**
     * 
     * @return 
     */
    private SecurityHandler configureSecurityHandler() {
        /**
         * Mostly from
         * https://github.com/jesperfj/jetty-secured-sample/blob/master/src/main/java/HelloWorld.java
         */
        
        HashLoginService l = new HashLoginService();
        
        for (Map.Entry<String, String> entry : authKeys.entrySet()){
            String username = entry.getKey();
            String password = entry.getValue();
            log.info("Configuring account for "+username);
            
            l.putUser(username, Credential.getCredential(password), new String[] {"user"});
            l.setName("myrealm");
        }
        
        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);
        constraint.setRoles(new String[]{"user"});
        constraint.setAuthenticate(true);
         
        ConstraintMapping cm = new ConstraintMapping();
        cm.setConstraint(constraint);
        cm.setPathSpec("/*");
        
        ConstraintSecurityHandler csh = new ConstraintSecurityHandler();
        csh.setAuthenticator(new BasicAuthenticator());
        csh.setRealmName("myrealm");
        csh.addConstraintMapping(cm);
        csh.setLoginService(l);
        
        return csh;
    }
    
}
