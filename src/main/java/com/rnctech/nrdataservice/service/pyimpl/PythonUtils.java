package com.rnctech.nrdataservice.service.pyimpl;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rnctech.nrdataservice.exception.TechException;
import com.rnctech.nrdataservice.service.RNResult;
import com.rnctech.nrdataservice.utils.RNCLogOutputStream;
import com.rnctech.nrdataservice.utils.ShellUtils;

import py4j.GatewayServer;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

/**
 * Python Utility for Gateway Server
 * @author zilin chen
 * @since 2020.10
 */

public class PythonUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(PythonUtils.class);

  public static GatewayServer createGatewayServer(Object entryPoint,
                                                  String serverAddress,
                                                  int port,
                                                  String secretKey,
                                                  boolean useAuth) throws IOException {
    LOGGER.info("Launching GatewayServer at " + serverAddress + ":" + port +
        ", useAuth: " + useAuth);
    if (useAuth) {
      try {
        Class clz = Class.forName("py4j.GatewayServer$GatewayServerBuilder", true,
            Thread.currentThread().getContextClassLoader());
        Object builder = clz.getConstructor(Object.class).newInstance(entryPoint);
        builder.getClass().getMethod("authToken", String.class).invoke(builder, secretKey);
        builder.getClass().getMethod("javaPort", int.class).invoke(builder, port);
        builder.getClass().getMethod("javaAddress", InetAddress.class).invoke(builder,
            InetAddress.getByName(serverAddress));
        builder.getClass()
            .getMethod("callbackClient", int.class, InetAddress.class, String.class)
            .invoke(builder, port, InetAddress.getByName(serverAddress), secretKey);
        return (GatewayServer) builder.getClass().getMethod("build").invoke(builder);
      } catch (Exception e) {
        throw new IOException(e);
      }
    } else {
      return new GatewayServer(entryPoint,
          port,
          GatewayServer.DEFAULT_PYTHON_PORT,
          InetAddress.getByName(serverAddress),
          InetAddress.getByName(serverAddress),
          GatewayServer.DEFAULT_CONNECT_TIMEOUT,
          GatewayServer.DEFAULT_READ_TIMEOUT,
          (List) null);
    }
  }

  public static String getLocalIP(Properties properties) {
    String gatewayserver_address =
        properties.getProperty("rnctech.gatewayserver_address");
    if (gatewayserver_address != null) {
      return gatewayserver_address;
    }

    try {
      return Inet4Address.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      LOGGER.warn("can't get local IP", e);
    }
    // fall back to loopback addreess
    return "127.0.0.1";
  }

  public static String createSecret(int secretBitLength) {
    SecureRandom rnd = new SecureRandom();
    byte[] secretBytes = new byte[secretBitLength / java.lang.Byte.SIZE];
    rnd.nextBytes(secretBytes);
    return Base64.encodeBase64String(secretBytes);
  }
  
  public static void main(String[] args) {
	    try {
			int port = ShellUtils.findFirstAvailablePort();
			Properties p = new Properties();
		    p.put("rnctech.maxResult", "20");
		    p.put("rnctech.gatewayserver_address", "127.0.0.1");		
			String serverAddress =  getLocalIP(p);
			String secret = "UAEHEEHsi4ANMN0FUJYNedqXHeD8wu2r6TENDNZ7mqY="; //PythonUtils.createSecret(256);
			System.out.println("PY4J_GATEWAY_SECRET="+secret);
			PythonExecutor pe = new PythonExecutor(p);
			RNCLogOutputStream outstream= new RNCLogOutputStream(LOGGER);
			pe.setOutputStream(outstream);
			GatewayServer gatewayServer = createGatewayServer(pe, serverAddress, port, secret,
			    true);
			gatewayServer.start();
			System.out.println("Gateway srver start at "+gatewayServer.getAddress()+":"+gatewayServer.getPort());
			pe.setGatewayServer(gatewayServer);
			 PythonContext pyctx = pe.getRNContext();
			//Thread t = new Thread() {
				Scanner s =new Scanner(System.in);
	        	System.out.println("Press quit to stop, other to exec .....\n");
				//public void run() {
					
			        pe.getPyScriptRunning().set(true);
			        while(true) {			        	
				        String ss = s.nextLine();
				        if(ss.equalsIgnoreCase("quit")) {
				        	pe.close();
				        	System.exit(1);
				        	return;
				        }else if(StringUtils.isBlank(ss)) {
				        	ss = "import sys\nprint(sys.version[0])";
				        }else {
				        	//for i in range(10):--newline----tab--print(i)
				        	//print('hello world')--newline--print('I\'m a dummy')
				        	ss = ss.replaceAll("--newline--", "\n").replaceAll("--tab--", "\t");
				        }
						try {
							System.out.println("call exec as: "+ss);
							RNResult result = pe.exec(ss, pyctx);
							System.out.println(result.getRetCode()+": "+result.toString());
						} catch (TechException e) {
							e.printStackTrace();
						}
			        }
				//}
				
			//};
			//t.start();
			
			
	    } catch (Exception e) {
			e.printStackTrace();
		}
  }
  
  
  
}

