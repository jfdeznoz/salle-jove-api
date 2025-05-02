package com.sallejoven.backend.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class S3Service {

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
        String filename = UUID.randomUUID() + "." + extension;
        String key = folderPath + "/" + filename;

        File localFile = convertToFile(file, filename);
        try {
            s3client.putObject(bucketName, key, localFile);
            log.info("Uploaded to S3: {}/{}", bucketName, key);
            return bucketUrl + key;
        } catch (AmazonServiceException e) {
            log.error("Failed to upload to S3: {}", e.getMessage());
            throw new RuntimeException("Error uploading file to S3", e);
        } finally {
            localFile.delete(); // Clean up
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
            // ✅ Subida normal SIN ACL
            s3client.putObject(bucketName, key, tempFile);
            log.info("Uploaded report to S3: {}/{}", bucketName, key);

            // ✅ Generamos URL firmada (válida por 15 minutos)
            Date expiration = new Date(System.currentTimeMillis() + 1000 * 60 * 15); // 15 minutos
            GeneratePresignedUrlRequest generatePresignedUrlRequest =
                    new GeneratePresignedUrlRequest(bucketName, key)
                            .withMethod(HttpMethod.GET)
                            .withExpiration(expiration);

            URL presignedUrl = s3client.generatePresignedUrl(generatePresignedUrlRequest);

            return presignedUrl.toString();

        } catch (AmazonServiceException e) {
            log.error("Failed to upload report to S3: {}", e.getMessage());
            throw new RuntimeException("Error uploading report file to S3", e);
        } finally {
            tempFile.delete(); // Cleanup
        }
    } 

    private AmazonS3 getClient() {
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        return AmazonS3ClientBuilder
                .standard()
                .withRegion(Regions.EU_NORTH_1) // Estocolmo
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .build();
    }

    private File convertToFile(MultipartFile file, String filename) throws IOException {
        File convertedFile = new File(System.getProperty("java.io.tmpdir") + "/" + filename);
        try (FileOutputStream fos = new FileOutputStream(convertedFile)) {
            fos.write(file.getBytes());
        }
        return convertedFile;
    }

    private String getExtension(String filename) {
        return filename != null && filename.contains(".") ? filename.substring(filename.lastIndexOf(".") + 1) : "bin";
    }
}