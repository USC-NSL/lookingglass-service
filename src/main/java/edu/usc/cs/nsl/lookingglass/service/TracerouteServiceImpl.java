package edu.usc.cs.nsl.lookingglass.service;

import edu.usc.cs.nsl.lookingglass.database.DBManager;
import edu.usc.cs.nsl.lookingglass.tracert.LGManager;
import edu.usc.cs.nsl.lookingglass.tracert.Query;
import java.util.LinkedList;
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
    
    public void start() throws Exception {
        log.info("Starting Traceroute Service and Query Processor.");
        queryProcessor.run();
    }
    
    public void stop() throws Exception {
        log.info("Shutting down Traceroute Service and Query Processor.");
        queryProcessor.shutdown();
    }

    public boolean isRunning() {
        return queryProcessor.isRunning();
    }

    @Override
    public boolean submit(Request request) {
        
        try {
            log.info("Processing request: "+request);
            
            String type = request.getType();
            String serverName = request.getLgName();
            String target = request.getTarget();
            
            //TODO fix this LinkedList nonsense for god sake
            LinkedList<Query> queries = new LinkedList<Query>();
            
            if(type == null || type.isEmpty()){            
                if(serverName.equalsIgnoreCase("*")){
                    queries = dbManager.createQueriesFromAllServers();
                } else {
                    queries.add(dbManager.createQueryFromServer(serverName));
                }
            } else if(type.equalsIgnoreCase("http")){
                if(serverName.equalsIgnoreCase("*")){
                    queries = dbManager.createQueriesFromAllHttpServers();
                } else {
                    queries.add(dbManager.createHttpQueryFromServer(serverName));
                }
            } else if(type.equalsIgnoreCase("telnet")){
                if(serverName.equalsIgnoreCase("*")){
                    queries = dbManager.createQueriesFromAllTelnetServers();
                } else {
                    queries.add(dbManager.createTelnetQueryFromServer(serverName));
                }
            }
            
            if(queries != null && queries.size() != 0){
                
                //set the target on all queries and add to query processor
                for(Query query : queries){
                    query.setTarget(target);
                    queryProcessor.submit(query);
                }
                
                return true;
            } else {
                log.error("No query could be constructed for request "+request);
                return false;
            }
            
        } catch(Exception ex){
            log.error("There was an error submitting the request "+request, ex);
            return false;
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
