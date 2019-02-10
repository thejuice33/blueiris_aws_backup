package bi.filewatcher;

import java.io.File;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

public class S3Upload {
	// Put your unique bucket name here
    String bucketName = "";
    // Put your access key here
    String accessKey = "";
    // Put your IAM role secret here
    String secret = "";
    
    public void upload(String fileName, String filePath) {

	    try {
	    	AWSCredentials credentials = new BasicAWSCredentials(
	    			accessKey, 
	    			  secret
	    			);
	    	
	        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
	                .withCredentials(new AWSStaticCredentialsProvider(credentials))
	                // Adjust region if necessary
	                .withRegion(Regions.US_EAST_1)
	                .build();
	    
	        // Upload a text string as a new object.
	        s3Client.putObject(bucketName, fileName, new File(filePath));
	        System.out.println("File backed up to cloud: " + fileName);

	        
	    }
	    catch(AmazonServiceException e) {
	        // The call was transmitted successfully, but Amazon S3 couldn't process 
	        // it, so it returned an error response.
	        e.printStackTrace();
	    }
	    catch(SdkClientException e2) {
	        // Amazon S3 couldn't be contacted for a response, or the client
	        // couldn't parse the response from Amazon S3.
	        e2.printStackTrace();
	    }
	}
}
