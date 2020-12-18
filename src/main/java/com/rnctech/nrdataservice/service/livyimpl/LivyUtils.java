package com.rnctech.nrdataservice.service.livyimpl;

import java.io.File;
import java.net.URI;

import org.apache.livy.LivyClient;
import org.apache.livy.LivyClientBuilder;

/**
 * @author zilin chen
 * @since 2020.10
 */

public class LivyUtils {

	public LivyUtils() {
	}

	public static void uploadlibs(String livyurl, String[] libs) throws Exception {
		LivyClient client = new LivyClientBuilder().setURI(new URI(livyurl)).build();
		// System.getProperty("java.class.path").split(File.pathSeparator)
		for (String s : libs) {
			if (new File(s).exists()) {
				client.uploadJar(new File(s)).get();
				break;
			}
		}

	}
}
