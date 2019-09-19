package jlambda.layer;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import org.apache.http.entity.ContentType;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * limit options for s3 here
 * keep everything encapsulated
 */
public enum S3LambdaClient {
    INSTANCE("S3LambdaClient"),
    BUCKET_NAME("lambda.pdf.report.447");

    private final String artifact;

    S3LambdaClient(String artifact) {
        this.artifact = artifact;
    }

    public String writeToBucket(String key, byte[]  dataBytes ) {

        ObjectMetadata metaData = new ObjectMetadata();
        metaData.setContentType(ContentType.DEFAULT_TEXT.toString());
        metaData.setContentEncoding(StandardCharsets.UTF_8.name());
        metaData.setContentLength(dataBytes.length);

        PutObjectRequest req = new PutObjectRequest(
                BUCKET_NAME.artifact,
                key,
                new ByteArrayInputStream(dataBytes),
                metaData
        );

        PutObjectResult ret = s3Client().putObject(req);
        return ret.getETag();

    }

    private AmazonS3 s3Client() {
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
        builder.setPathStyleAccessEnabled(true);
        return builder.build();
    }



}
