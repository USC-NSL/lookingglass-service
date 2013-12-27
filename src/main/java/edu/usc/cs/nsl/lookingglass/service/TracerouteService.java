/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.usc.cs.nsl.lookingglass.service;

import java.util.List;

/**
 *
 * @author matt
 */
public interface TracerouteService {
    
    boolean submit(Request request);
    
    List<String> active();
}
