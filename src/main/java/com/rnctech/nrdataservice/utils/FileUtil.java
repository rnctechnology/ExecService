package com.rnctech.nrdataservice.utils;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FileUtils;

/**
 * @author zilin chen
 * @since 2020.10
 */

public class FileUtil {

	public FileUtil() {
	}
	
	public static File createTempFile(String name, String extension) throws IOException {
		File tmpfile;
		try {
			File tmpfolder = FileUtils.getTempDirectory();
			tmpfile = File.createTempFile(name, extension, tmpfolder);
		} catch (IOException e) {
			tmpfile = File.createTempFile(name, extension);
		}
		return tmpfile;
	}

    public static boolean deleteTempFile (String fileName) {
    	File temporaryFile = new File(fileName);
    	return FileDeleteStrategy.FORCE.deleteQuietly(temporaryFile);
    }
    
	public static String saveFile(String filepath, String content) throws IOException {		
		File f = new File(filepath);
		//byte[] ctxbytes = content.getBytes();
		//FileUtils.writeByteArrayToFile(f, ctxbytes);
		FileUtils.write(f, content, false);
		return f.getAbsolutePath();
	}
}
