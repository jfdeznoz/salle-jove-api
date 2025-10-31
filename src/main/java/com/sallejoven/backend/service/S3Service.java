package com.sallejoven.backend.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class S3Service {
/*
    @Value("${salle.aws.access-key}")
    private String accessKey;

    @Value("${salle.aws.secret-key}")
    private String secretKey;

    @Value("${salle.aws.bucket-name}")
    private String bucketName;

    @Value("${salle.aws.bucket-url}")
    private String bucketUrl;

    public String uploadFile(MultipartFile file, String folderPath) throws IOException {
        AmazonS3 s3client = getClient();

        String extension = getExtension(file.getOriginalFilename());
        String filename  = UUID.randomUUID() + "." + extension;
        String key       = folderPath + "/" + filename;

        File localFile = convertToFile(file, filename);
        try {
            s3client.putObject(bucketName, key, localFile);
            log.info("Uploaded to S3: {}/{}", bucketName, key);
            return bucketUrl + key;
        } catch (AmazonServiceException e) {
            log.error("Failed to upload to S3: {}", e.getMessage());
            throw new RuntimeException("Error uploading file to S3", e);
        } finally {
            localFile.delete();
        }
    }

    public String uploadFileReport(ByteArrayOutputStream stream, String folderPath, String filename) throws IOException {
        AmazonS3 s3client = getClient();

        File tempFile = new File(System.getProperty("java.io.tmpdir") + "/" + filename);
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            stream.writeTo(fos);
        }

        String key = folderPath + "/" + filename;
        try {
            // Subida
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(tempFile.length());
            meta.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            s3client.putObject(bucketName, key, new FileInputStream(tempFile), meta);
            log.info("Uploaded report to S3: {}/{}", bucketName, key);

            // Genera URL prefirmada (15 min)
            Date expiration = new Date(System.currentTimeMillis() + 1000 * 60 * 15);
            GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(bucketName, key)
                    .withMethod(HttpMethod.GET)
                    .withExpiration(expiration);
            URL presignedUrl = s3client.generatePresignedUrl(req);
            return presignedUrl.toString();

        } catch (AmazonServiceException e) {
            log.error("Failed to upload report to S3: {}", e.getMessage());
            throw new RuntimeException("Error uploading report file to S3", e);
        } finally {
            tempFile.delete();
        }
    }

    public byte[] downloadFile(String key) {
        AmazonS3 s3client = getClient();

        try (InputStream inputStream = s3client.getObject(bucketName, key).getObjectContent();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }

            return outputStream.toByteArray();

        } catch (IOException | AmazonS3Exception e) {
            log.error("Error downloading file from S3: {}", e.getMessage());
            throw new RuntimeException("Error downloading file from S3", e);
        }
    }

    public boolean exists(String key) {
        AmazonS3 s3client = getClient();
        return s3client.doesObjectExist(bucketName, key);
    }

    public String getFileUrl(String key) {
        return bucketUrl + key;
    }

    // — Helpers internos —

    private AmazonS3 getClient() {
        AWSCredentials creds = new BasicAWSCredentials(accessKey, secretKey);
        return AmazonS3ClientBuilder.standard()
                .withRegion(Regions.EU_NORTH_1)
                .withCredentials(new AWSStaticCredentialsProvider(creds))
                .build();
    }

    private File convertToFile(MultipartFile file, String filename) throws IOException {
        File convFile = new File(System.getProperty("java.io.tmpdir") + "/" + filename);
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
        }
        return convFile;
    }

    private String getExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf(".") + 1);
        }
        return "bin";
    }
*/

}