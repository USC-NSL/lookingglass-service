/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.usc.cs.nsl.lookingglass.service;

/**
 *
 * @author matt
 */
public interface TracerouteService {

    boolean isRunning();
    
    void start() throws Exception;
    
    void stop() throws Exception;
    
    boolean submit(Request request);
    
}
