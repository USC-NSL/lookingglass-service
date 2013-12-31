package edu.usc.cs.nsl.lookingglass.service;

import edu.usc.cs.nsl.lookingglass.tracert.ClientThread;
import edu.usc.cs.nsl.lookingglass.tracert.DomainInfo;
import edu.usc.cs.nsl.lookingglass.tracert.HttpClientThread;
import edu.usc.cs.nsl.lookingglass.tracert.HttpQuery;
import edu.usc.cs.nsl.lookingglass.tracert.LGManager;
import edu.usc.cs.nsl.lookingglass.tracert.Query;
import edu.usc.cs.nsl.lookingglass.tracert.TelnetClientThread;
import edu.usc.cs.nsl.lookingglass.tracert.TelnetQuery;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;

/**
 *
 * @author matt calder
 */
public class QueryProcessor {
    
    private static final String POISON_PILL = "POISON PILL";
    private static final String THREADPOOL_NAME = "QueryProcessorThread";
    private static Logger log = Logger.getLogger(QueryProcessor.class);
    private BlockingQueue Q;
    private LGManager lgManager;
    private long queuePollTimeout = 1000;
    private int threadpoolSize = 20;
    private int qSize = 100;
    private int domainQueryTimeGap = 5*60*1000;
    //private ExecutorService executor;
    private ThreadPoolExecutor executor;
    private boolean isRunning = false;
    
    /**
     * 
     * @param lgManager 
     */
    public QueryProcessor(LGManager lgManager) {
        this.lgManager = lgManager;
        //Q = new ArrayBlockingQueue(qSize);
        Q = new LinkedBlockingQueue();
    }
    
    /**
     * 
     * @param query
     * @return 
     */
    public boolean submit(Query query){
        if(!isRunning){
            throw new RuntimeException("QueryProcessor is not running.");
        }
        return Q.add(query);
    }
    
    /**
     * 
     * @param measurementId
     * @return 
     */
    public boolean inQueue(int measurementId) {

        /**
         * The LinkedBlockingQueue implementation makes this pseudo-thread safe.
         */
        
        for (Object o : Q) {
            try {
                Query query = (Query) o;
                if (measurementId == query.getMeasurementId()) {
                    return true;
                }
            } catch (ClassCastException ex) {
                //if this is a string, POISON_PILL, then just continue
                continue;
            }
        }

        return false;

    }
    
    /**
     * Please use this method as a last resort.
     * 
     * @param measurementId
     * @return 
     */
    public boolean isExecuting(int measurementId) {
        
        /**
         * The LinkedBlockingQueue implementation makes this pseudo-thread safe.
         */
        
        if (executor == null) {
            return false;
        }

        BlockingQueue<Runnable> queue = executor.getQueue();
        Iterator<Runnable> iter = queue.iterator();
        
        while(iter.hasNext()){
            try {
              ClientThread clientThread = (ClientThread)iter.next();             
              if(measurementId == clientThread.getQuery().getMeasurementId()){
                  return true;
              }
            } catch(ClassCastException ex){
                //must have gotten unlucky and found the POISON_PILL!
                continue;
            }
        }
        
        return false;
    }
    
    /**
     * 
     * @throws Exception 
     */
    public void run() throws Exception {
        
        log.info("Started QueryProcessor");
        
        //executor = Executors.newFixedThreadPool(threadpoolSize, new QueryProcessorThreadFactory(THREADPOOL_NAME));
        executor = new ThreadPoolExecutor(threadpoolSize, threadpoolSize,
                0L, TimeUnit.MILLISECONDS, 
                new LinkedBlockingQueue<Runnable>());
        
        isRunning = true;
        
        while(true){
            
            Object object = Q.poll(queuePollTimeout, TimeUnit.MILLISECONDS);
            
            if(object != null) {
                if(object instanceof String){
                    //this must be poison pill
                    log.info("Got poison pill. Shutting down query processor");
                    executor.shutdown();
                    isRunning = false; //should already be set
                    break;
                } else {
                    /**
                     * This must be a legit traceroute query so
                     * add to the lgManager
                     */
                    Query query = (Query)object;
                    log.info("Adding query "+query+" to LGManager");
                    lgManager.addQuery(query);
                }
            } else {
                //log.debug("Q poll timeout");
            }
            
            /**
             * Got here because we either got a query or a timeout
             */
            runAvailableQueries();
        }
        
    }
    
    /**
     * 
     */
    public void runAvailableQueries() {
        
        /**
         * The plan is to do away with this entire method but the lookingglass
         * library needs further refactoring before that can happen. Namely, LGManager
         * doesn't do anything very important and should be replaced.
         */
        
        //log.debug("runAvailableQueries");
        
        for (String domain : lgManager.getQueries().keySet()) {

            if (lgManager.getQueries().get(domain).size() == 0) {
                continue;
            }

            int timeGap = domainQueryTimeGap;
            if (domain.indexOf("eunetip") != -1) {
                timeGap = 200000;
            } else if (domain.indexOf("sprint") != -1) {
                timeGap = 40000;
            }

            boolean canStart = false;
            
            DomainInfo domainInfo = lgManager.getDomainInfos().get(domain);
            if (domainInfo == null) {
                log.error("Error! domain was not found in domainInfos: " + domain);
                continue;
            }

            canStart = !domainInfo.getInPool()
                    && new Date().getTime() - domainInfo.getFinishTime() > timeGap;
            
            if (canStart) {
                domainInfo.start(); //marks this as being in the thread pool
                
                Query query = lgManager.removeQuery(domain);

                if (query instanceof HttpQuery) {
                    executor.execute(new HttpClientThread((HttpQuery) query, lgManager));
                } else if (query instanceof TelnetQuery) {
                    executor.execute(new TelnetClientThread((TelnetQuery) query, lgManager));
                }
            }
            
        }
    }
    
    /**
     * 
     * @return 
     */
    public BlockingQueue getQ() {
        return Q;
    }

    /**
     * 
     * @param Q 
     */
    public void setQ(BlockingQueue Q) {
        this.Q = Q;
    }

    /**
     * 
     * @return 
     */
    public int getThreadpoolSize() {
        return threadpoolSize;
    }

    /**
     * 
     * @param threadpoolSize 
     */
    public void setThreadpoolSize(int threadpoolSize) {
        this.threadpoolSize = threadpoolSize;
    }

    /**
     * 
     * @return 
     */
    public long getQueuePollTimeout() {
        return queuePollTimeout;
    }
    
    /**
     * 
     * @param queuePollTimeout 
     */
    public void setQueuePollTimeout(long queuePollTimeout) {
        this.queuePollTimeout = queuePollTimeout;
    }
    
    /**
     * 
     * @param executor 
     */
    //public void setExecutor(ExecutorService executor) {
    //    this.executor = executor;
    // }
    
    /**
     * 
     * @return 
     */
    public ExecutorService getExecutor() {
        return executor;
    }
    
    /**
     * 
     * @return 
     */
    public int getQSize() {
        return qSize;
    }
    
    /**
     * 
     * @param qSize 
     */
    public void setQSize(int qSize) {
        this.qSize = qSize;
    }
    
    /**
     * Successive calls do nothing.
     */
    public void shutdown(){
        if(!isRunning){
            log.error("QueryProcessor is not running.");
            return;
        }
        
        Q.add(POISON_PILL);
        isRunning = false;
    }
    
    public boolean isRunning(){
        return isRunning;
    }
    
//    private class QueryProcessorThreadFactory implements ThreadFactory {
//            
//        private int counter = 0;
//        private String prefix;
//
//        public QueryProcessorThreadFactory(String prefix) {
//            this.prefix = prefix;
//        }
//        
//        public Thread newThread(Runnable r) {
//            return new Thread(r, prefix+'-'+counter++);
//        }
//    }
}