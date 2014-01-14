package edu.usc.cs.nsl.lookingglass.service;

import edu.usc.cs.nsl.lookingglass.database.DBManager;
import edu.usc.cs.nsl.lookingglass.database.QueryLog;
import edu.usc.cs.nsl.lookingglass.tracert.Query;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author matt
 */
public class TracerouteServiceImpl implements TracerouteService {
    
    private final static Logger log = Logger.getLogger(TracerouteServiceImpl.class);

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
        log.info("Process request for active looking glasses");
        return dbManager.activeLGs();
    }
    
    /**
     * 
     * @param measurementId
     * @return 
     */
    public List<TracerouteResult> results(int measurementId) {
        
        log.info("Processing request for results of measurementId: "+measurementId);
        
        List<QueryLog> queries = dbManager.getMeasurements(measurementId);
               
        List<TracerouteResult> results = new ArrayList<TracerouteResult>();
        for(QueryLog query : queries){
            
            TracerouteResult result = new TracerouteResult();
            result.setLgName(query.getServerName());
            result.setStatus(databaseStatusToService(query.getState()));
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
        
        log.info("Processing request for status of measurementId: "+measurementId);
        
        /**
         * First check the queue
         */
        if(queryProcessor.inQueue(measurementId)){
            return "processing";
        }
        
        /**
         * Next we check the database
         */
        String status = getDatabaseStatus(measurementId);
        log.info("Measurement Id: "+measurementId+" DB query 1 status: "+status);
        
        if(status.equals("not found")){
            /**
             * If not found in database, there is a small chance that it
             * could be in the thread pool and the database hasn't been updated yet.
             */
            if(queryProcessor.isExecuting(measurementId)){
                log.info("Found "+measurementId+" in QueryProcessor");
                return "processing";
            }
            
            /**
             * It hurts, but need to check the database again to confirm that the
             * thread didn't start and finish processing while we were the database above.
             */
            status = getDatabaseStatus(measurementId);
            log.info("Measurement Id: "+measurementId+" DB query 2 status: "+status);
        }
        
        log.info("Returning Measurement Id: "+measurementId+" status as: "+status);
        return status;
    }
    
    /**
     * 
     * @author matt calder
     * @param measurementId
     * @return 
     */
    public String getDatabaseStatus(int measurementId){
        
        List<String> selectFields = new ArrayList<String>();
        selectFields.add("intKey");
        selectFields.add("state");
        
        List<String> whereClauses = new ArrayList<String>();
        whereClauses.add("measurement_id = "+measurementId);
        
        List<QueryLog> queryLogs = dbManager.findQueryLog(selectFields, whereClauses);
        
        if(queryLogs.isEmpty()){
            return "not found";
        }
        
        /**
         * We count how many failures we see to determine whether this is a 
         * complete failure or a partial failure.
         */
        int failCount = 0; 
        
        for(QueryLog queryLog : queryLogs){
            String state = queryLog.getState();
            int id = queryLog.getIdKey();
            
            log.info("mId "+measurementId+" id "+id+" status "+state);
            
            if(state.equalsIgnoreCase("Unfinished")){
                //if even one is incomplete then the measurement is unfinished
                return "unfinished";
            } else if(state.equalsIgnoreCase("Fail")){
                failCount++;
            }
        }
        
        if(failCount > 0){
            //we have at least one failure
            if(failCount == queryLogs.size()){
                return "failed";
            } else {
                //must be some failed and some have not
                return "some failed";
            }
        } else {
            return "finished";
        }
    }
    
    /**
     * 
     * @param asn
     * @return 
     */
    @Override
    public Collection<String> active(int asn) {
       log.info("Processing request for active LGs in ASN "+asn);
       return dbManager.getASMapping().get(asn);
    }

    /**
     * 
     * @return 
     */
    @Override
    public Collection<Integer> ases() {
        log.info("Processing request for ASes");
        return dbManager.getASMapping().keySet();
    }
    
    public String databaseStatusToService(String dbStatus){
        if(dbStatus.equalsIgnoreCase("Success")){
            return "finished";
        } else if(dbStatus.equalsIgnoreCase("Fail")){
            return "failed";
        } else if(dbStatus.equalsIgnoreCase("Unfinished")){
            return "unfinished";
        } else {
            log.error("Got unknown dbStatus: "+dbStatus);
            return null;
        }
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