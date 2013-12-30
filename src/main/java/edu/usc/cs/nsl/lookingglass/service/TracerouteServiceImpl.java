package edu.usc.cs.nsl.lookingglass.service;

import edu.usc.cs.nsl.lookingglass.database.DBManager;
import edu.usc.cs.nsl.lookingglass.tracert.Query;
import edu.usc.cs.nsl.lookingglass.tracert.TracerouteInfo;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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
