package edu.usc.cs.nsl.lookingglass.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.googlecode.jsonrpc4j.StreamServer;
import java.io.IOException;
import java.net.InetAddress;
import org.apache.log4j.Logger;

public class LookingGlassServer 
{
    private static Logger log = Logger.getLogger(LookingGlassServer.class);
    
    //http://stackoverflow.com/questions/14631502/jsonrpc4j-how-to-add-type-info-to-parameters
    
    private int port;
    private int maxThreads;
    private StreamServer streamServer;
    private TracerouteService tracerouteService;
    
    /**
     * 
     * @param tracerouteService
     * @param port
     * @param maxThreads 
     */
    public LookingGlassServer(TracerouteService tracerouteService, int port, int maxThreads) {
        this.tracerouteService = tracerouteService;
        this.port = port;
        this.maxThreads = maxThreads;
    }
    
    /**
     * 
     * @throws IOException 
     */
    public void start() throws Exception {
        log.info("Starting server..");
        
        JsonRpcServer rpcServer = new JsonRpcServer(new ObjectMapper(), tracerouteService);
        
        streamServer = new StreamServer(rpcServer, this.maxThreads, this.port, 50, InetAddress.getByName("127.0.0.1"));
        
        streamServer.start();
        log.info("Server started.");
        
        tracerouteService.start();
    }
    
    /**
     * 
     * @throws Exception 
     */
    public void stop() throws Exception {
        log.info("Shutting down server");
        
        if(tracerouteService != null){
            tracerouteService.stop();
        }
        
        if (streamServer != null && streamServer.isStarted()){
            streamServer.stop();
        }
       
    }

    public TracerouteService getTracerouteService() {
        return tracerouteService;
    }

    public void setTracerouteService(TracerouteService tracerouteService) {
        this.tracerouteService = tracerouteService;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }
    
}
