package edu.usc.cs.nsl.lookingglass.service;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import edu.usc.cs.nsl.lookingglass.database.DBManager;
import edu.usc.cs.nsl.lookingglass.tracert.LGManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.apache.log4j.Logger;

/**
 *
 * @author matt
 */
public class Main {
    
    private static Logger log = Logger.getLogger(Main.class);
    
    //command line args
    @Parameter(names = "-config", description = "Path to configuration file", required = true)
    private String configFile;

    public String getConfigFile() {
        return configFile;
    }

    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }
    
    public static void main(String args[]) throws Exception {
        
        Main main = new Main();
        
        JCommander cmd = new JCommander(main); //load command line args
        try {
            cmd.parse(args); //parse command line args
        } catch(ParameterException ex){
            cmd.usage();
            return;
        }
        
        Properties prop = new Properties(); //load a properties file
        try {
            prop.load(new FileInputStream(main.getConfigFile()));
        } catch (IOException ex) {
            System.err.println("There was an error loading the config file: "+main.getConfigFile());
            return;
        }
        
        //this stuff is suspicious..
        String timeout = "240000";
        System.setProperty("sun.net.client.defaultReadTimeout", timeout);
        
        String driverName = prop.getProperty("db.driver");
        String url = prop.getProperty("db.url");
        String username = prop.getProperty("db.user");
        String password = prop.getProperty("db.password");
        
        final DBManager dbManager = new DBManager(url, username, password, driverName);
        LGManager lgManager = new LGManager(dbManager);
        
        int port = Integer.parseInt(prop.getProperty("server.port", "1420"));
        int maxThreads = Integer.parseInt(prop.getProperty("server.maxThreads", "50"));
        
        QueryProcessor queryProcessor = new QueryProcessor(lgManager);
        TracerouteService tracerouteService = new TracerouteServiceImpl(dbManager, queryProcessor);
        
        //final LookingGlassServer server = new LookingGlassServer(tracerouteService, port, maxThreads);
        final LookingGlassHttpServer server = new LookingGlassHttpServer(port, tracerouteService, queryProcessor);
        
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    server.stop();
                } catch(Exception e){
                    log.error("Got error trying to stop server in shutdown hook", e);
                }
            }
        });
        
        server.start();  //this will block
    }
    
}
