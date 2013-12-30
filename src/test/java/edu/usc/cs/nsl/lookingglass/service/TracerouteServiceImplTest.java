package edu.usc.cs.nsl.lookingglass.service;

import edu.usc.cs.nsl.lookingglass.database.DBManager;
import edu.usc.cs.nsl.lookingglass.tracert.HttpQuery;
import edu.usc.cs.nsl.lookingglass.tracert.Query;
import edu.usc.cs.nsl.lookingglass.tracert.TelnetQuery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import static org.junit.Assert.*;

/**
 *
 * @author matt
 */
public class TracerouteServiceImplTest {
    
    public TracerouteServiceImplTest() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of submit method, of class TracerouteServiceImpl.
     */
    @Test
    public void testSubmit() throws Exception {
        
        System.out.println("submit");
        
        QueryProcessor queryProcessor = Mockito.mock(QueryProcessor.class);
        DBManager dbManager = Mockito.mock(DBManager.class);
        Query query = Mockito.mock(Query.class);
        HttpQuery httpQuery = Mockito.mock(HttpQuery.class);
        TelnetQuery telnetQuery = Mockito.mock(TelnetQuery.class);
        
        Mockito.when(dbManager.createQueryFromServer("Comcast")).thenReturn(query);
        Mockito.when(dbManager.createHttpQueryFromServer("Comcast")).thenReturn(httpQuery);
        Mockito.when(dbManager.createTelnetQueryFromServer("Comcast")).thenReturn(telnetQuery);
        Mockito.when(dbManager.createQueryFromServer("Foobar")).thenReturn(null);
        
        final TracerouteServiceImpl instance = new TracerouteServiceImpl(dbManager, queryProcessor);
         
        int result1 = instance.submit("Comcast", "www.google.com", null);
        assertTrue(result1 >= 0);
        Mockito.verify(dbManager).createQueryFromServer("Comcast");
        Mockito.verify(queryProcessor).submit(query);
        
        int result2 = instance.submit("Comcast", "www.google.com", "http");
        assertTrue(result2 >= 0);
        Mockito.verify(dbManager).createHttpQueryFromServer("Comcast");
        Mockito.verify(queryProcessor).submit(httpQuery);
        
        int result3 = instance.submit("Comcast", "www.google.com", "telnet");
        assertTrue(result3 >= 0);
        Mockito.verify(dbManager).createTelnetQueryFromServer("Comcast");
        Mockito.verify(queryProcessor).submit(telnetQuery);
        
        Mockito.reset(queryProcessor, dbManager);
        
        int result4 = instance.submit("Comcast", "www.google.com", "foobar");
        assertEquals(-1, result4);
        Mockito.verifyZeroInteractions(dbManager, queryProcessor);
        
        //instance.stop();
    }
}