package edu.usc.cs.nsl.lookingglass.service;

import edu.usc.cs.nsl.lookingglass.tracert.HttpQuery;
import edu.usc.cs.nsl.lookingglass.tracert.LGManager;
import edu.usc.cs.nsl.lookingglass.tracert.Query;
import java.util.ArrayList;
import java.util.Collection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mockito.Mockito;

/**
 *
 * @author matt
 */
public class QueryProcessorTest {
    
    private LGManager lgManager;
    
    public QueryProcessorTest() {
    }
    
    @Before
    public void setUp() {
        lgManager = Mockito.mock(LGManager.class);
        
        Collection<String> domains = new ArrayList<String>();
        Mockito.when(lgManager.getDomains()).thenReturn(domains);
    }
    
    @After
    public void tearDown() {
        lgManager = null;
    }

    /**
     * Test of submit method, of class QueryProcessor.
     */
    @Test
    public void testSubmit() throws Exception {
        System.out.println("submit");
        
        Query query = Mockito.mock(HttpQuery.class);
        
        final QueryProcessor instance = new QueryProcessor(lgManager);
        //instance.setQ(new SynchronousQueue());
        
        try {
            instance.submit(query);
            fail("Expected exception because query processor isn't running.");
        } catch(Exception e){
            //expect to get here
        }
        
        new Thread(){
            @Override
            public void run() {
                try{
                    instance.run();
                } catch(Exception e){
                    System.err.println(e);
                }
            }
        }.start();
       
        Thread.sleep(1000); //let the thread get started
        
        instance.submit(query);
        
        //give the Q consumer thread time to pull object off
        Thread.sleep(500);
        
        Mockito.verify(lgManager).addQuery(query);
        
        instance.shutdown();
    }

    /**
     * Test of run method, of class QueryProcessor.
     */
    @Test
    public void testRun() throws Exception {
        System.out.println("run");
        
        final QueryProcessor instance = new QueryProcessor(lgManager);
        //instance.setQ(new SynchronousQueue());
        instance.setQueuePollTimeout(500); //500 ms
        
        new Thread(){
            @Override
            public void run() {
                try {
                    instance.run();
                } catch(Exception e){
                    System.err.println(e);
                }
            }
        }.start();
        Thread.sleep(1000); //let thread get started
        
        Thread.sleep(1000*2); //should be invoked 4 times
        
        /*
         * The Q should timeout 4 times, so we check that lgManager.getQueries 
         * is called at least 3 times since the timing is rough.
         */
        Mockito.verify(lgManager, Mockito.atLeast(3)).getDomains();
        
        instance.shutdown();
    }
    
    /**
     * Test of shutdown method, of class QueryProcessor.
     */
    @Test
    public void testShutdown() throws Exception {
        
        final QueryProcessor instance = new QueryProcessor(lgManager);
        //instance.setQ(new SynchronousQueue());
        
        assertFalse(instance.isRunning());
        
        new Thread(){
            @Override
            public void run() {
                try {
                    instance.run();
                } catch(Exception e){
                    System.err.println(e);
                }
            }
        }.start();
        
        Thread.sleep(1000);
        assertTrue(instance.isRunning());
        
        instance.shutdown();
        
        assertFalse(instance.isRunning());
    }
}