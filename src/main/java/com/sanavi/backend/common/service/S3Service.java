package com.sanavi.backend.common.service;

import java.io.IOException;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.region}")
    private String region;

    // Input: file, folder (예: "match"), savedName (UUID_원본명)
    // Output: S3 객체 key (presigned URL 발급 시 재사용)
    // 책임: S3 업로드 후 key 반환 — filePath 컬럼에 key를 저장해 presigned URL에 활용
    public String upload(MultipartFile file, String folder, String savedName) throws IOException {
        if (file == null || file.isEmpty()) {
            log.warn(
                    "action=S3_UPLOAD object_prefix={} result=DENIED reason=EMPTY_FILE",
                    folder);

            throw new IllegalArgumentException("빈 파일은 업로드할 수 없습니다.");
        }

        String key = folder + "/" + savedName;

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .contentLength(file.getSize())
                            .build(),
                    RequestBody.fromBytes(file.getBytes()));

            log.debug(
                    "action=S3_UPLOAD object_prefix={} file_size={} content_type={} result=SUCCESS",
                    folder,
                    file.getSize(),
                    file.getContentType());

            return key;
        } catch (IOException e) {
            log.error(
                    "action=S3_UPLOAD object_prefix={} file_size={} content_type={} result=FAIL reason=FILE_READ_FAILED exception={}",
                    folder,
                    file.getSize(),
                    file.getContentType(),
                    e.getClass().getSimpleName());

            throw e;
        } catch (RuntimeException e) {
            log.error(
                    "action=S3_UPLOAD object_prefix={} file_size={} content_type={} result=FAIL reason=S3_PUT_FAILED exception={}",
                    folder,
                    file.getSize(),
                    file.getContentType(),
                    e.getClass().getSimpleName());

            throw e;
        }
    }

    // Input: key (S3 객체 key), expiresInMinutes (URL 유효 시간)
    // Output: 서명된 임시 다운로드 URL
    // 책임: 비공개 버킷 파일을 일정 시간 동안만 접근 가능한 URL로 발급
    public String generatePresignedUrl(String key, int expiresInMinutes) {
        String objectPrefix = extractObjectPrefix(key);

        try (S3Presigner presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build()) {

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expiresInMinutes))
                    .getObjectRequest(req -> req.bucket(bucket).key(key))
                    .build();

            String url = presigner.presignGetObject(presignRequest).url().toString();

            log.debug(
                    "action=S3_PRESIGNED_URL_CREATE object_prefix={} expires_in_minutes={} result=SUCCESS",
                    objectPrefix,
                    expiresInMinutes);

            return url;
        } catch (RuntimeException e) {
            log.error(
                    "action=S3_PRESIGNED_URL_CREATE object_prefix={} expires_in_minutes={} result=FAIL exception={}",
                    objectPrefix,
                    expiresInMinutes,
                    e.getClass().getSimpleName());

            throw e;
        }
    }

    private String extractObjectPrefix(String key) {
        if (key == null || key.isBlank()) {
            return "-";
        }

        int index = key.lastIndexOf("/");

        if (index == -1) {
            return "-";
        }

        return key.substring(0, index);
    }
}