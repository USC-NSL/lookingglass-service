package edu.usc.cs.nsl.lookingglass.service;

import java.util.List;

/**
 *
 * @author matt calder
 */
public interface TracerouteService {
    
    /**
     * 
     * @param lgName
     * @param target
     * @param type
     * @return a measurement id
     */
    int submit(String lgName, String target, String type);
    
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
    
    /**
     * 
     * @param measurementId
     * @return 
     */
    String status(int measurementId);
    
    /**
     * 
     * @param measurementId
     * @return 
     */
    List<TracerouteResult> results(int measurementId);
    
}
