package edu.usc.cs.nsl.lookingglass.service;

import java.util.List;

/**
 *
 * @author matt calder
 */
public interface TracerouteService {
    
    /**
     * 
     * @param request
     * @return a measurement id
     */
    int submit(Request request);
    
    /**
     * Finds all active Looking Glasses based on their most recent status.
     * @return 
     */
    List<String> active();
    
    /**
     * Finds all active Looking Glasses in an AS.
     * @param ASN only the AS Number, 
     * @return
     */
    List<String> active(int asn);
    
    
}
