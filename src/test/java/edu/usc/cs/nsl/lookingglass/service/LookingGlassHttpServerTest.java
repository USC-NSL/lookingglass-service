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
 * @author matt calder
 */
public class LookingGlassHttpServerTest {
    
    private int port = 1420;
    //private LookingGlassHttpServer server;
    private String serverName;
    private String target;
    private String type;
    
    public LookingGlassHttpServerTest() {
        serverName = "Comcast";
        target = "www.google.com";
        type = "http";
    }
    
    @Before
    public void setUp() throws Exception {
       
    }
    
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test of run method, of class LookingGlassServer.
     */
    @Test
    public void testRemoteCall() throws Exception {
        System.out.println("remoteCall");

        TracerouteService tracerouteService = new TracerouteServiceTestImpl();
        QueryProcessor queryProcessor = Mockito.mock(QueryProcessor.class);
        
        final LookingGlassHttpServer server = new LookingGlassHttpServer(port, tracerouteService, queryProcessor);
        
        Thread serverThread = new Thread(){
            @Override
            public void run() {
                try {
                    server.start();
                } catch(Exception ex){
                    throw new RuntimeException(ex);
                }
            } 
        };
        serverThread.start();
        
        Thread.sleep(1000); //lolh4x. sleep for a second to let server thread get going.
        
        JsonRpcHttpClient client = new JsonRpcHttpClient(new URL("http://127.0.0.1:"+port+"/lg"));

        TracerouteService remoteService = ProxyUtil.createClientProxy(
                getClass().getClassLoader(),
                TracerouteService.class,
                client);

        int result = remoteService.submit(serverName, target, type);
        assertEquals(100, result);
        
        server.stop();
        serverThread.join();  //wait for thread to die
    }
    
    public class TracerouteServiceTestImpl implements TracerouteService {

        public List<String> active(int asn) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public List<String> active() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public List<TracerouteResult> results(int measurementId) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public String status(int measurementId) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
        @Override
        public int submit(String thisServerName, String thisTarget, String thisType) {
            if (thisServerName.equals(serverName) &&
                thisType.equals(type)){
                return 100;
            } else {
                return -1;
            }
        }      
    }
}