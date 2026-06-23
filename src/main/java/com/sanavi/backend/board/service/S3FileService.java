package com.sanavi.backend.board.service;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.sanavi.backend.board.dto.BoardFileDto;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class S3FileService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    public BoardFileDto uploadBoardFile(int boardId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("빈 파일은 업로드할 수 없습니다.");
        }

        String originalName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = getExtension(originalName);
        String savedName = UUID.randomUUID() + extension;
        String key = "board/" + boardId + "/" + savedName;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(
                    request,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );

            BoardFileDto dto = new BoardFileDto();
            dto.setBoardId(boardId);
            dto.setOriginalName(originalName);
            dto.setSavedName(savedName);
            dto.setFilePath(key);
            dto.setFileSize(file.getSize());

            return dto;
        } catch (IOException e) {
            throw new RuntimeException("S3 파일 업로드에 실패했습니다.", e);
        }
    }

    public byte[] download(String key) {
        ResponseBytes<GetObjectResponse> objectBytes =
                s3Client.getObjectAsBytes(builder -> builder
                        .bucket(bucket)
                        .key(key)
                        .build());

        return objectBytes.asByteArray();
    }

    private String getExtension(String filename) {
        int index = filename.lastIndexOf(".");
        if (index == -1) {
            return "";
        }

        return filename.substring(index);
    }
}