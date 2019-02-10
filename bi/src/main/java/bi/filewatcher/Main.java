package bi.filewatcher;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Main {
	private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
	private static final String BLUE_IRIS_PATH = "C:\\BlueIris\\New";
	private static final String LOG_FILE = "C:\\Users\\pc\\Desktop\\monitoring\\upload.log";
	private static final String CACHE_DIR = "C:\\BlueIris\\cache\\";
	
	public static void main(String[] args) {
		//define a folder root
        Path myDir = Paths.get(BLUE_IRIS_PATH);    
        FileHandler fh;  
        
        try {  

            // This block configure the logger with handler and formatter  
            fh = new FileHandler(LOG_FILE);  
            LOGGER.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();  
            fh.setFormatter(formatter);  


        } catch (SecurityException e) {  
            e.printStackTrace();  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
        

        try {
           WatchService watcher = myDir.getFileSystem().newWatchService();
           myDir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, 
           StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);

           WatchKey key;
           while ((key = watcher.take()) != null) {
        	    for (WatchEvent<?> event : key.pollEvents()) {
        	    	
        	    	if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE && 
        	    			!fileAlreadyUploaded(event.context().toString()) &&
        	    			!file247(event.context().toString())) {
        	    		LOGGER.info("New Motion Alert Video Detected: " + event.context().toString());  
        	    		String fileLocation = myDir.toString() + "/" + event.context().toString();
    	    			File lockFile = new File(fileLocation);
         	            if(isFileWritten(lockFile)) {
         	            	LOGGER.info("Recording complete. Uploading [" + event.context().toString() + "] to cloud...");  
         	            	uploadFile(lockFile, event, fileLocation);
         	            	LOGGER.info(event.context().toString() + " - Uploaded to cloud");  
         	            	createCacheRecord(event.context().toString());
         	            }       	           
                    }else if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY && 
                    		!fileAlreadyUploaded(event.context().toString()) &&
                    		!file247(event.context().toString())) {
        	    		String fileLocation = myDir.toString() + "/" + event.context().toString();
        	            File lockFile = new File(fileLocation);
        	            if(!fileAlreadyUploaded(event.context().toString()) && isFileWritten(lockFile)) {
        	            	LOGGER.info("Recording complete. Uploading [" + event.context().toString() + "] to cloud...");  
        	            	uploadFile(lockFile, event, fileLocation);
        	            	createCacheRecord(event.context().toString());
        	            }  
                    }else {
                    	//LOGGER.log(Level.INFO, "File: " + event.context().toString() + " - was either uploaded already, still being written or is a 24x7 file");
                    }
                    
        	    }
        	    key.reset();
        	}
           
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error: " + e.toString());
        }
    }
	
	/**
	 * Checks if the file is finished being written.
	 * 
	 * @param file
	 * @return
	 */
	private static boolean isFileWritten(File file) {
		RandomAccessFile raf = null;
        try {
        	raf = new RandomAccessFile(file, "rw");
        	return true;
        }catch (Exception e) {
        	LOGGER.info("Recording still in progress.  Waiting...");  
        }finally {
    	  if (raf != null) {
              try {
            	  raf.close();
              } catch (IOException e) {
            	  LOGGER.log(Level.SEVERE, "Exception during closing file " + file.getName());
              }
          }
        }
        return false;
	}
	
	/**
	 * Uploads to AWS S3.
	 * 
	 * @param lockFile
	 * @param event
	 * @param fileLocation
	 * @throws Exception
	 */
	private static void uploadFile(File lockFile, WatchEvent<?> event, String fileLocation) throws Exception{
		S3Upload s3Upload = new S3Upload();
		
		RandomAccessFile raf = new RandomAccessFile(lockFile, "rw");
   	 	FileChannel channel = raf.getChannel();
        channel.lock();
        raf.close();
        s3Upload.upload(event.context().toString(), fileLocation);
	}
	
	/**
	 * Creates a cache record of this file being uploaded.  Blue Iris seems to finish writing to a file and 
	 * subsequently updates it a short period after.  This prevents duplicate file uploads when a file update
	 * re-triggers the Windows file watcher event.
	 * @param fileName
	 */
	private static void createCacheRecord(String fileName) {
		List<String> lines = Arrays.asList("The first line", "The second line");
		Path file = Paths.get(CACHE_DIR + fileName);
		try {
			Files.write(file, lines, Charset.forName("UTF-8"));
		}catch (IOException ioe){
			LOGGER.log(Level.SEVERE, "Failed writing record file: " + fileName);
		}		
	}
	
	/**
	 * Determines from cache if file has already been uploaded.
	 * 
	 * @param fileName
	 * @return
	 */
	private static boolean fileAlreadyUploaded(String fileName) {
		File tempFile = new File(CACHE_DIR + fileName);
		
		return tempFile.exists();
	}
	
	/**
	 * A quick hack to prevent 24x7 recorded video streams from being uploaded.  Just add "24by7" into the file name
	 * of any camera that records continuously.  Better ways to do this...just a quick fix.
	 * 
	 * @param fileName
	 * @return
	 */
	private static boolean file247(String fileName) {		
		boolean twentyfourx7 = fileName.contains("24by7");
		return twentyfourx7;
	}
}
