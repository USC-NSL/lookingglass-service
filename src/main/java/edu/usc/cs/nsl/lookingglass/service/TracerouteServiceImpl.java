package edu.usc.cs.nsl.lookingglass.service;

import edu.usc.cs.nsl.lookingglass.database.DBManager;
import edu.usc.cs.nsl.lookingglass.database.QueryLog;
import edu.usc.cs.nsl.lookingglass.tracert.Query;
import edu.usc.cs.nsl.lookingglass.tracert.TracerouteInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;

/**
 *
 * @author matt
 */
public class TracerouteServiceImpl implements TracerouteService {
    
    private final static Logger log = Logger.getLogger(TracerouteServiceImpl.class);
    
    private Pattern asPattern;
    private QueryProcessor queryProcessor;
    private DBManager dbManager;
    
    /**
     * 
     * @param dbManager
     * @param queryProcessor 
     */
    public TracerouteServiceImpl(DBManager dbManager, QueryProcessor queryProcessor) {
        this.dbManager = dbManager;
        this.queryProcessor = queryProcessor;
        this.asPattern = Pattern.compile("AS[0-9]+");
    }
    
    @Override
    public int submit(String serverName, String target, String type) {
        
        String request = "serverName: "+serverName+" target: "+target+" type: "+type;
        
        try {
            log.info("Processing request: "+request);
            
            //TODO fix this LinkedList nonsense for god sake
            LinkedList<Query> queries = new LinkedList<Query>();
            
            if(type == null || type.isEmpty()){            
                if(serverName.equalsIgnoreCase("*")){
                    queries = dbManager.createQueriesFromAllServers();
                } else {
                    Query query = dbManager.createQueryFromServer(serverName);
                    if(query != null){
                        queries.add(query);
                    }
                }
            } else if(type.equalsIgnoreCase("http")){
                if(serverName.equalsIgnoreCase("*")){
                    queries = dbManager.createQueriesFromAllHttpServers();
                } else {
                    Query query = dbManager.createHttpQueryFromServer(serverName);
                    if(query != null){
                        queries.add(query);
                    }
                }
            } else if(type.equalsIgnoreCase("telnet")){
                if(serverName.equalsIgnoreCase("*")){
                    queries = dbManager.createQueriesFromAllTelnetServers();
                } else {
                    Query query = dbManager.createTelnetQueryFromServer(serverName);
                    if(query != null){
                        queries.add(query);
                    }
                }
            }
            
            if(queries != null && queries.size() != 0){
                
                int measurementId = dbManager.getNextMeasurementId(System.currentTimeMillis());
                log.info("Request "+request+" has measurementId: "+measurementId);
                
                //set the target on all queries and add to query processor
                for(Query query : queries){
                    query.setTarget(target);
                    query.setMeasurementId(measurementId);
                    queryProcessor.submit(query);
                }
                
                return measurementId;
            } else {
                log.error("No query could be constructed for request "+request);
                return -1;
            }
            
        } catch(Exception ex){
            log.error("There was an error submitting the request "+request, ex);
            return -1;
        }
    }
    
    @Override
    public List<String> active() {
        TracerouteInfo trInfo = new TracerouteInfo(dbManager);
        return trInfo.findWorkingVPs();
    }
    
    /**
     * 
     * @param measurementId
     * @return 
     */
    public List<TracerouteResult> results(int measurementId) {
        
        List<QueryLog> queries = dbManager.getMeasurements(measurementId);
               
        List<TracerouteResult> results = new ArrayList<TracerouteResult>();
        for(QueryLog query : queries){
            
            TracerouteResult result = new TracerouteResult();
            result.setLgName(query.getServerName());
            result.setStatus(query.getState());
            result.setTarget(query.getTarget());
            
            String parsedData = query.getParsedData();
            String[] hopsArray = parsedData.split("\n");
            List<String> hopsList = Arrays.asList(hopsArray);
            result.setHops(hopsList);
            
            results.add(result);
        }
        
        return results;
    }

    /**
     * 
     * @param measurementId
     * @return 
     */
    public String status(int measurementId) {
        
        /**
         * First check the queue.
         */
        if(queryProcessor.inQueue(measurementId)){
            return "processing";
        }
        
        /**
         * Next we check the database
         */
        TracerouteInfo trInfo = new TracerouteInfo(dbManager);
        String status = trInfo.getMeasurementStatus(measurementId);
        
        if(status.equals("not found")){
            /**
             * If not found in database, there is a small chance that it
             * could be in the thread pool and the database hasn't been updated yet.
             */
            if(queryProcessor.isExecuting(measurementId)){
                return "processing";
            }  
            
            /**
             * It hurts, but need to check the database again to confirm that the
             * thread didn't start and finish processing while we were the database above.
             */
            status = trInfo.getMeasurementStatus(measurementId);
        }
        
        return status;
    }
    
    /**
     * 
     * @param asn
     * @return 
     */
    @Override
    public List<String> active(int asn) {
       
       List<String> activeLgNames = active();
       Pattern p = Pattern.compile("AS"+asn);
       
       List<String> matches = new ArrayList<String>();
       
       for(String lgName : activeLgNames){
            Matcher matcher = p.matcher(lgName);
            if(matcher.find()){
                matches.add(lgName);
            }
       }
       
       return matches;
    }

    /**
     * 
     * @return 
     */
    @Override
    public Set<Integer> ases() {
        
        Set<Integer> asSet = new HashSet<Integer>();
        
        List<String> activeLgNames = active();
        for(String lgName : activeLgNames){
            Matcher matcher = asPattern.matcher(lgName);
            if(matcher.find()){
                String asString = matcher.group();   //return first match for now
                try {
                    Integer asn = Integer.parseInt(asString.substring(2));
                    
                    //filter out all reserved AS numbers
                    if (asn == 0){
                        continue;
                    }
                    
                    asSet.add(asn);
                } catch(Exception e){
                    log.error("Got error parsing ASN out of "+asString, e);
                }
            }
        }
        
        return asSet;
    }
    
    public DBManager getDbManager() {
        return dbManager;
    }

    public void setDbManager(DBManager dbManager) {
        this.dbManager = dbManager;
    }

    public QueryProcessor getQueryProcessor() {
        return queryProcessor;
    }

    public void setQueryProcessor(QueryProcessor queryProcessor) {
        this.queryProcessor = queryProcessor;
    }
    
}