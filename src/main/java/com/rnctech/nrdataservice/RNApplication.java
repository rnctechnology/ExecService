package com.rnctech.nrdataservice;

import java.net.Socket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.AbstractEnvironment;
import com.rnctech.nrdataservice.config.AppConfig;
import com.rnctech.nrdataservice.config.FileConfig;
import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.springframework.MariaDB4jSpringService;

/**
 * @contributor zilin
 * 2020.09
 */

@SpringBootApplication(exclude = org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class)
public class RNApplication {
    
	public static final String INMEMORYDB_SETUP = "immemorydb_setup";
	public static final String INMEMORYDB_PORT = "immemorydb_port";
	public static final String PYENGINE_SETUP = "pyengine_setup";
	public static boolean isPyEngine = false;
	
	@Autowired
	AppConfig appconfig;
	
	@Autowired
	FileConfig fconfig;
	
	static MariaDB4jSpringService DB;
	static int dbport = 3307;
	
	public static void main(String[] args) {
        System.setProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, "dev");
        System.setProperty("jasypt.encryptor.password", "rnctech123!");
        try {
			init();
		} catch (ManagedProcessException e) {
			e.printStackTrace();
		}
        SpringApplication app = new SpringApplication(RNApplication.class);
        app.addListeners(new ApplicationErrorListener());
        app.run();
    }
	
    public static void init() throws ManagedProcessException {
        boolean imMemoryDB = false;       
        try {
            	if(null != System.getProperty(PYENGINE_SETUP)) {
            		isPyEngine = Boolean.parseBoolean(System.getProperty(PYENGINE_SETUP).toLowerCase());
            	}else if (null != System.getenv(PYENGINE_SETUP)) {
            		isPyEngine = Boolean.parseBoolean(System.getenv(PYENGINE_SETUP).toLowerCase());
            	}
            System.out.println("Set isPyEngine as "+isPyEngine());
        }catch(Exception e) {}
    	
        try {
        	if(null != System.getProperty(INMEMORYDB_SETUP)) {
        		imMemoryDB = Boolean.parseBoolean(System.getProperty(INMEMORYDB_SETUP).toLowerCase());
        	}else if (null != System.getenv(INMEMORYDB_SETUP)) {
        		imMemoryDB = Boolean.parseBoolean(System.getenv(INMEMORYDB_SETUP).toLowerCase());
        	}
        }catch(Exception e) {}
        if(imMemoryDB) { 
	        Socket s = null;
	        try {
	        	try {
	        	if(null != System.getProperty(INMEMORYDB_PORT)) {
	        		dbport = Integer.parseInt(System.getProperty(INMEMORYDB_PORT));
	        	}else if(null != System.getenv(INMEMORYDB_PORT)) {
	        		dbport = Integer.parseInt(System.getenv(INMEMORYDB_PORT));
	        	}}catch(Throwable t) {}
	            s = new Socket("localhost", dbport);
	            System.out.println(dbport+" port already listened.");
	        } catch (Exception e){
	        	DB = new MariaDB4jSpringService();
	        	DB.setDefaultPort(dbport);
	            DB.start();
	            DB.getDB().createDB("rnjob");
	            DB.getDB().source("db/changelog/sql/tables_mariadb.sql");
	            DB.getDB().source("db/changelog/sql/rn_mariadb.sql");
	        }finally{
	            if(s != null)
	                try {s.close();}
	                catch(Exception e){}
	        }	        
	        Runtime.getRuntime().addShutdownHook(new Thread() {
	         	   public void run() {
	         		   System.out.println("shutdown hook...");
	         	       DB.stop();
	         	   }});
        }
    }
    
	public static boolean isPyEngine() {
		return isPyEngine;
	}
	
    public void run(String... args) throws Exception {
        System.out.println("using environment: " + appconfig.getEnv());
        System.out.println("file folder: "+fconfig.getUploadDir());
    }
    
	public static class ApplicationErrorListener implements ApplicationListener<ApplicationFailedEvent> {
		@Override
		public void onApplicationEvent(ApplicationFailedEvent event) {
			if (event.getException() != null) {
				System.out.println("Stoping application as exception: "+event.getException().getMessage());
				if(null != DB) DB.stop();
				event.getApplicationContext().close();
				System.exit(-1);
			}
		}
	}
}
