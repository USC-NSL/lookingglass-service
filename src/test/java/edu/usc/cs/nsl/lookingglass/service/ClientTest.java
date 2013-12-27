package edu.usc.cs.nsl.lookingglass.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcClient;
import com.googlecode.jsonrpc4j.ProxyUtil;
import edu.usc.cs.nsl.lookingglass.service.Request;
import edu.usc.cs.nsl.lookingglass.service.TracerouteService;
import java.net.InetAddress;
import java.net.Socket;

/**
 *
 * @author matt
 */
public class ClientTest {
    
    public static void main(String args[]) throws Throwable {
        
        JsonRpcClient jsonRpcClient = new JsonRpcClient(new ObjectMapper());
        
        TracerouteService remoteService = ProxyUtil.createClientProxy(
            TracerouteService.class.getClassLoader(),
            TracerouteService.class, jsonRpcClient,
            new Socket(InetAddress.getByName("127.0.0.1"), 1420));
        
        Request request = new Request("*", "8.8.8.8", "http");
        int measurementId = remoteService.submit(request);
        
        System.out.println(measurementId);
    }
    
}
