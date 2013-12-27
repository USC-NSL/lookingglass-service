/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.usc.cs.nsl.lookingglass.service;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;
import java.net.URL;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

/**
 *
 * @author matt
 */
public class LookingGlassHttpServerTest {
    
    private int port = 1420;
    private LookingGlassHttpServer server;
    private Request request;
    
    public LookingGlassHttpServerTest() {
    }
    
    @Before
    public void setUp() throws Exception {
        //TracerouteService tracerouteService = Mockito.mock(TracerouteService.class);
        request = new Request("Comcast", "www.google.com", "http");
        
        TracerouteService tracerouteService = new TracerouteServiceTestImpl();
        QueryProcessor queryProcessor = Mockito.mock(QueryProcessor.class);
        
        server = new LookingGlassHttpServer(port, tracerouteService, queryProcessor);
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

        JsonRpcHttpClient client = new JsonRpcHttpClient(new URL("http://127.0.0.1:"+port+"/lg"));

        TracerouteService remoteService = ProxyUtil.createClientProxy(
                getClass().getClassLoader(),
                TracerouteService.class,
                client);

        boolean result = remoteService.submit(request);
        assertEquals(true, result);
    }
    
    public class TracerouteServiceTestImpl implements TracerouteService {

        public List<String> active() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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