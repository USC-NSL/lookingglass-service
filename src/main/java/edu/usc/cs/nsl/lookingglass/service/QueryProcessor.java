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
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.log4j.Logger;
import org.eclipse.jetty.util.ConcurrentHashSet;

/**
 *
 * @author matt calder
 */
public class QueryProcessor {
    
    private static final String POISON_PILL = "POISON PILL";
    private static Logger log = Logger.getLogger(QueryProcessor.class);
    private BlockingQueue Q;
    private LGManager lgManager;
    private Set<Object> processing;
    private long queuePollTimeout = 1000;
    private int threadpoolSize = 100;
    private int qSize = 100;
    private int domainQueryTimeGap = 5*60*1000;
    private ThreadPoolExecutor executor;
    private boolean isRunning;
    private Lock lock;
    
    private long lastTimeQueriesChecked = 0;
    private long newQueriesCheck = 3000;
    
    /**
     * 
     * @param lgManager 
     */
    public QueryProcessor(LGManager lgManager) {
        this.lgManager = lgManager;
        this.isRunning = false;
        //Q = new ArrayBlockingQueue(qSize);
        Q = new LinkedBlockingQueue();
        processing = new ConcurrentHashSet<Object>();
        lock = new ReentrantLock();
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
        
        lock.lock();
        try {
            return Q.add(query);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 
     * @param measurementId
     * @return 
     */
    public boolean inQueue(int measurementId) {

        lock.lock();
        try {
            /**
             * The LinkedBlockingQueue implementation makes this pseudo-thread
             * safe.
             */
            log.info("Examining " + Q.size() + " queue entries");
            for (Object o : Q) {
                try {
                    Query query = (Query) o;
                    log.info("Checking " + query);
                    if (measurementId == query.getMeasurementId()) {
                        return true;
                    }
                } catch (ClassCastException ex) {
                    //if this is a string, POISON_PILL, then just continue
                    log.error("Got exception while looking for measurement id " + measurementId + " in processing queue", ex);
                    continue;
                }
            }
            
            for(Object o : processing){
                if(o instanceof Query){
                    Query q = (Query)o;
                    if(q.getMeasurementId() == measurementId){
                        return true;
                    }
                }
            }
            
            log.info("Now checking LGManager");
            return lgManager.contains(measurementId);
        } finally {
            lock.unlock();
        }
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

        lock.lock();
        try {

            BlockingQueue<Runnable> queue = executor.getQueue();
            Iterator<Runnable> iter = queue.iterator();

            log.info("Examining " + queue.size() + " queue entries");
            while (iter.hasNext()) {
                try {
                    ClientThread clientThread = (ClientThread) iter.next();
                    log.info("Checking " + clientThread);

                    if (measurementId == clientThread.getQuery().getMeasurementId()) {
                        return true;
                    }
                } catch (Exception ex) {
                    log.error("Got exception while looking for measurement id " + measurementId + " in executor pool", ex);
                    //must have gotten unlucky and found the POISON_PILL!
                    continue;
                }
            }

            return false;
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 
     * @throws Exception 
     */
    public void run() throws Exception {
        
        log.info("Started QueryProcessor");
        
        //executor = Executors.newFixedThreadPool(threadpoolSize, new QueryProcessorThreadFactory(THREADPOOL_NAME));
        executor = new ThreadPoolExecutor(20, threadpoolSize,
                1000L, TimeUnit.MILLISECONDS, 
                new LinkedBlockingQueue<Runnable>());
        
        isRunning = true;
        
        while (true) {

            Object object = Q.poll(queuePollTimeout, TimeUnit.MILLISECONDS);
            
            if (object != null) {
                
                processing.add(object);
                
                if (object instanceof String) {
                    //this must be poison pill
                    processing.remove(object);
                    log.info("Got poison pill. Shutting down query processor");
                    executor.shutdown();
                    isRunning = false; //should already be set
                    break;
                } else {
                    /**
                     * This must be a legit traceroute query so add to the
                     * lgManager
                     */
                    Query query = (Query) object;
                    
                    /**
                     * Adding this to the lgManager requires database calls so
                     * we put it on a thread to process
                     */
                    new Thread(new AddQueryRunnable(query)).start();
                }
            } else {
                //log.debug("Q poll timeout");
            }
            
            /**
             * Got here because we either got a query or a timeout
             */
            if((System.currentTimeMillis()-lastTimeQueriesChecked) > this.newQueriesCheck){
                runAvailableQueries();
            }
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
        
        for (String domain : lgManager.getDomains()) {

            if (lgManager.numberOfQueries(domain) == 0) {
                continue;
            }

            int timeGap = domainQueryTimeGap;
            if (domain.indexOf("eunetip") != -1) {
                timeGap = 200000;
            } else if (domain.indexOf("sprint") != -1) {
                timeGap = 40000;
            }

            
            DomainInfo domainInfo = lgManager.getDomainInfo(domain);
            if (domainInfo == null) {
                log.error("Error! domain was not found in domainInfos: " + domain);
                continue;
            }

//            canStart = !domainInfo.getInPool()
//                    && new Date().getTime() - domainInfo.getFinishTime() > timeGap;
            boolean canStart = false;
            
            if(domainInfo.getLastRunTime() == -1){
                canStart = true; //first time running this domain
            } else {
                canStart = (System.currentTimeMillis()-domainInfo.getLastRunTime()) >= timeGap;
            }
            
            if (canStart) {
                domainInfo.start(); //marks this as being in the thread pool
                domainInfo.setLastRunTime(System.currentTimeMillis());
                
                lock.lock();
                try {
                    Query query = lgManager.removeQuery(domain);

                    if (query instanceof HttpQuery) {
                        executor.execute(new HttpClientThread((HttpQuery) query, lgManager));
                    } else if (query instanceof TelnetQuery) {
                        executor.execute(new TelnetClientThread((TelnetQuery) query, lgManager));
                    }
                } finally {
                    lock.unlock();
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
    
    private class AddQueryRunnable implements Runnable {

        private Query query;
        public AddQueryRunnable(Query query) {
            this.query = query;
        }
        
        public void run() {
            log.info("Adding query " + query + " to LGManager");
            lgManager.addQuery(query);
            processing.remove(query);
        }
    }
}