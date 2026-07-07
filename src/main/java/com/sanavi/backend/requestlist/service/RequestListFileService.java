package com.sanavi.backend.requestlist.service;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.sanavi.backend.requestlist.dto.DirectRequestFileDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestListFileService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    public DirectRequestFileDto uploadRequestFile(int matchId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn(
                    "action=DIRECT_REQUEST_FILE_UPLOAD target_type=match target_id={} result=DENIED reason=EMPTY_FILE",
                    matchId);

            throw new IllegalArgumentException("빈 파일은 업로드할 수 없습니다.");
        }

        String originalName = file.getOriginalFilename();

        if (originalName == null || originalName.isBlank()) {
            originalName = "file";
        }

        originalName = StringUtils.cleanPath(originalName);

        String fileType = getFileType(originalName);
        String savedName = UUID.randomUUID() + "." + fileType;
        String key = "requestlist/" + matchId + "/" + savedName;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            DirectRequestFileDto dto = new DirectRequestFileDto();
            dto.setMatchId(matchId);
            dto.setOriginalName(originalName);
            dto.setSavedName(savedName);
            dto.setFilePath(key);
            dto.setFileSize(file.getSize());
            dto.setFileType(fileType);

            log.debug(
                    "action=DIRECT_REQUEST_FILE_UPLOAD target_type=match target_id={} object_key={} file_size={} file_type={} result=SUCCESS",
                    matchId,
                    key,
                    file.getSize(),
                    fileType);

            return dto;
        } catch (IOException e) {
            log.error(
                    "action=DIRECT_REQUEST_FILE_UPLOAD target_type=match target_id={} object_key={} file_size={} file_type={} result=FAIL exception={}",
                    matchId,
                    key,
                    file.getSize(),
                    fileType,
                    e.getClass().getSimpleName());

            throw new RuntimeException("의뢰 첨부파일 업로드에 실패했습니다.", e);
        }
    }

    private String getFileType(String filename) {
        int index = filename.lastIndexOf(".");

        if (index == -1 || index == filename.length() - 1) {
            return "file";
        }

        String extension = filename.substring(index + 1).toLowerCase();

        if (extension.length() > 10) {
            return extension.substring(0, 10);
        }

        return extension;
    }

    public byte[] download(String key) {
        try {
            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(builder -> builder
                    .bucket(bucket)
                    .key(key)
                    .build());

            byte[] data = objectBytes.asByteArray();

            log.debug(
                    "action=DIRECT_REQUEST_FILE_DOWNLOAD object_key={} file_size={} result=SUCCESS",
                    key,
                    data.length);

            return data;
        } catch (RuntimeException e) {
            log.error(
                    "action=DIRECT_REQUEST_FILE_DOWNLOAD object_key={} result=FAIL exception={}",
                    key,
                    e.getClass().getSimpleName());

            throw e;
        }
    }
}