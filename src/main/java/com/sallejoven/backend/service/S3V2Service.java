package com.sallejoven.backend.service;

import com.sallejoven.backend.model.dto.PresignedPutDTO;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Year;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class S3V2Service {

    private final S3Client s3;
    private final S3Presigner presigner;

    @Value("${salle.aws.bucket-name}") private String bucket;
    @Value("${salle.aws.bucket-url}")  private String bucketUrl;
    @Value("${salle.aws.prefix:}")     private String basePrefix;

    public PresignedPut presignedPut(String key,
                                     @Nullable String contentType,
                                     @Nullable String contentDisposition,
                                     boolean publicRead,
                                     Duration ttl) {
        PutObjectRequest.Builder putBuilder = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType((contentType == null || contentType.isBlank()) ? null : contentType)
                .contentDisposition((contentDisposition == null || contentDisposition.isBlank()) ? null : contentDisposition);

        // OJO: si el bucket está en BucketOwnerEnforced, NO se permiten ACLs y esto dará error.
        if (publicRead) {
            putBuilder.acl(ObjectCannedACL.PUBLIC_READ);
        }

        var put = putBuilder.build();

        var pre = presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                        .signatureDuration(ttl)
                        .putObjectRequest(put)
                        .build()
        );

        var flatHeaders = pre.signedHeaders().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> String.join(",", e.getValue())
                ));

        return new PresignedPut(pre.url().toString(), key, flatHeaders);
    }

    public String presignedGet(String key, Duration ttl) {
        var get = GetObjectRequest.builder().bucket(bucket).key(key).build();
        var pre = presigner.presignGetObject(GetObjectPresignRequest.builder()
                .getObjectRequest(get)
                .signatureDuration(ttl)
                .build());
        return pre.url().toString();
    }

    public boolean exists(String key) {
        try {
            s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (S3Exception e) {
            return false;
        }
    }

    public void putBytes(String key, byte[] bytes, String contentType) {
        var req = PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build();
        s3.putObject(req, RequestBody.fromBytes(bytes));
    }

    public String publicUrl(String key) {
        return (bucketUrl.endsWith("/") ? bucketUrl : bucketUrl + "/") + key;
    }

    public String eventFolderForEvent(boolean isGeneral, @Nullable Long centerId, int year, long eventId) {
        String scope = isGeneral ? "general" : "centers/" + centerId;
        String folder = join(basePrefix, String.valueOf(year), "events", scope, "event_" + eventId);
        return folder.isEmpty() ? "" : folder + "/";
    }

    public String eventFolder(boolean isGeneral, @Nullable Long centerId) {
        String scope = isGeneral ? "general/events" : "centers/" + centerId + "/events";
        String folder = join(basePrefix, scope);
        return folder.isEmpty() ? "" : folder + "/";
    }

    private String join(String... parts) {
        return Arrays.stream(parts)
                .filter(p -> p != null && !p.isBlank())
                .map(p -> p.replaceAll("^/+", "").replaceAll("/+$", ""))
                .collect(Collectors.joining("/"));
    }

    public String newKey(String prefix, @Nullable String suffix) {
        String file = UUID.randomUUID() + (suffix == null ? "" : suffix);
        if (prefix == null || prefix.isBlank()) return file;
        return (prefix.endsWith("/") ? prefix : prefix + "/") + file;
    }

    public record PresignedPut(String url, String key, Map<String,String> requiredHeaders) {}

    public UploadPresigneds buildPresignedForEventUploads(
            boolean isGeneral,
            @Nullable Long centerId,
            @Nullable LocalDate eventDate,
            long eventId,
            boolean wantImage,
            boolean wantPdf,
            @Nullable String imageOriginalName
    ) {
        final int year = (eventDate != null) ? eventDate.getYear() : Year.now().getValue();
        final String folder = eventFolderForEvent(isGeneral, centerId, year, eventId);

        PresignedPutDTO imgDto = null;
        PresignedPutDTO pdfDto = null;

        if (wantImage) {
            String ext = extFromFilename(imageOriginalName);
            if (ext.isBlank()) ext = "jpg";
            String mime = mimeFromExt(ext);

            String imgKey = newKey(folder, "." + ext);
            String cd    = "inline; filename=\"" + sanitizeFilename(
                    (imageOriginalName == null || imageOriginalName.isBlank())
                            ? ("cover." + ext) : imageOriginalName) + "\"";

            var imgPut = presignedPut(imgKey, mime, cd, /*publicRead*/ false, Duration.ofMinutes(15));
            imgDto = new PresignedPutDTO(imgPut.url(), imgPut.key(), imgPut.requiredHeaders());
        }

        if (wantPdf) {
            String pdfKey = newKey(folder, ".pdf");
            String cd     = "inline; filename=\"documento.pdf\"";
            var pdfPut = presignedPut(pdfKey, "application/pdf", cd, /*publicRead*/ false, Duration.ofMinutes(15));
            pdfDto = new PresignedPutDTO(pdfPut.url(), pdfPut.key(), pdfPut.requiredHeaders());
        }

        return new UploadPresigneds(imgDto, pdfDto);
    }

    public record UploadPresigneds(@Nullable PresignedPutDTO image, @Nullable PresignedPutDTO pdf) {}

    public void deleteObject(String key) {
        try {
            s3.deleteObject(b -> b.bucket(bucket).key(key));
        } catch (S3Exception ignored) {
        }
    }

    public String keyFromUrl(String url) {
        if (url == null || url.isBlank()) return null;
        String base = bucketUrl.endsWith("/") ? bucketUrl : bucketUrl + "/";
        if (url.startsWith(base)) {
            return url.substring(base.length());
        }
        return url;
    }

    public static String extFromFilename(@Nullable String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot == name.length() - 1) return "";
        return name.substring(dot + 1).toLowerCase();
    }

    public static String mimeFromExt(String ext) {
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png"         -> "image/png";
            case "webp"        -> "image/webp";
            case "gif"         -> "image/gif";
            case "pdf"         -> "application/pdf";
            default            -> "application/octet-stream";
        };
    }

    public static String sanitizeFilename(String s) {
        String base = s.replace('\\','/');
        base = base.substring(base.lastIndexOf('/') + 1);
        return base.replaceAll("[\"\\r\\n]", "_");
    }
}