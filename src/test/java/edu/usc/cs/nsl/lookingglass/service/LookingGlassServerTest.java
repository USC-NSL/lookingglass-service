/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.usc.cs.nsl.lookingglass.service;

import edu.usc.cs.nsl.lookingglass.service.Request;
import edu.usc.cs.nsl.lookingglass.service.LookingGlassServer;
import edu.usc.cs.nsl.lookingglass.service.TracerouteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcClient;
import com.googlecode.jsonrpc4j.ProxyUtil;
import edu.usc.cs.nsl.lookingglass.tracert.Query;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

/**
 *
 * @author matt
 */
public class LookingGlassServerTest {
    
    private int port = 1420;
    private int threads = 1;
    private LookingGlassServer server;
    private Request request;
    
    public LookingGlassServerTest() {
    }
    
    @Before
    public void setUp() throws Exception {
        //TracerouteService tracerouteService = Mockito.mock(TracerouteService.class);
        request = new Request("Comcast", "www.google.com", "http");
        
        TracerouteService tracerouteService = new TracerouteServiceTestImpl(); 
        server = new LookingGlassServer(tracerouteService, port, threads);
        server.start();
    }
    
    @After
    public void tearDown() throws Exception {
        server.stop();
        request = null;
    }

    /**
     * Test of run method, of class LookingGlassServer.
     */
    @Test
    public void testRemoteCall() throws Exception {
        System.out.println("remoteCall");
        
        JsonRpcClient jsonRpcClient = new JsonRpcClient(new ObjectMapper());
        TracerouteService remoteService = ProxyUtil.createClientProxy(
            TracerouteService.class.getClassLoader(),
            TracerouteService.class, jsonRpcClient,
            new Socket(InetAddress.getByName("127.0.0.1"), port));
        
        boolean result = remoteService.submit(request);
        assertEquals(true, result);
    }
    
    public class TracerouteServiceTestImpl implements TracerouteService {
        
        @Override
        public boolean isRunning() {
            return true;
        }

        @Override
        public void start() throws Exception {
        }
        
        @Override
        public void stop() throws Exception {
        }

        @Override
        public boolean submit(Request r1) {
            if (r1.getLgName().equals(request.getLgName()) &&
                r1.getType().equals(request.getType())){
                return true;
            } else {
                return false;
            }
        }      
    }
}