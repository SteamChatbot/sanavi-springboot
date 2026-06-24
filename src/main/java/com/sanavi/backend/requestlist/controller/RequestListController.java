package com.sanavi.backend.requestlist.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sanavi.backend.common.response.ApiResponse;
import com.sanavi.backend.requestlist.dto.DirectRequestCreateDto;
import com.sanavi.backend.requestlist.dto.DirectRequestFileDto;
import com.sanavi.backend.requestlist.dto.DirectRequestResponseDto;
import com.sanavi.backend.requestlist.dto.LawyerDetailResponseDto;
import com.sanavi.backend.requestlist.dto.LawyerListResponseDto;
import com.sanavi.backend.requestlist.dto.ReceivedRequestResponseDto;
import com.sanavi.backend.requestlist.dto.RequestRejectDto;
import com.sanavi.backend.requestlist.dto.SentRequestResponseDto;
import com.sanavi.backend.requestlist.service.RequestListService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/requestlist")
public class RequestListController {

    private final RequestListService requestListService;

    @GetMapping("/lawyers")
    public ResponseEntity<ApiResponse<List<LawyerListResponseDto>>> getLawyerList() {
        List<LawyerListResponseDto> response = requestListService.getLawyerList();

        return ResponseEntity.ok(
                ApiResponse.success("변호사 목록 조회 성공", response));
    }

    @GetMapping("/lawyers/{lawyerId}")
    public ResponseEntity<ApiResponse<LawyerDetailResponseDto>> getLawyerDetail(
            @PathVariable String lawyerId) {
        LawyerDetailResponseDto response = requestListService.getLawyerDetail(lawyerId);

        return ResponseEntity.ok(
                ApiResponse.success("변호사 상세 조회 성공", response));
    }

    @PostMapping(value = "/requests", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Integer>>> createDirectRequest(
            @ModelAttribute DirectRequestCreateDto requestDto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        int matchId = requestListService.createDirectRequest(requestDto, files);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "의뢰 요청이 전송되었습니다.",
                        Map.of("matchId", matchId)));
    }

    @GetMapping("/requests/sent")
    public ResponseEntity<ApiResponse<List<SentRequestResponseDto>>> getSentRequests(
            @RequestParam String userId) {
        List<SentRequestResponseDto> response = requestListService.getSentRequests(userId);

        return ResponseEntity.ok(
                ApiResponse.success("보낸 의뢰 목록 조회 성공", response));
    }

    @GetMapping("/requests/received")
    public ResponseEntity<ApiResponse<List<ReceivedRequestResponseDto>>> getReceivedRequests(
            @RequestParam String lawyerId) {
        List<ReceivedRequestResponseDto> response = requestListService.getReceivedRequests(lawyerId);

        return ResponseEntity.ok(
                ApiResponse.success("받은 의뢰 목록 조회 성공", response));
    }

    @GetMapping("/requests/{matchId}")
    public ResponseEntity<ApiResponse<DirectRequestResponseDto>> getDirectRequestDetail(
            @PathVariable int matchId) {
        DirectRequestResponseDto response = requestListService.getDirectRequestDetail(matchId);

        return ResponseEntity.ok(
                ApiResponse.success("의뢰 상세 조회 성공", response));
    }

    @PatchMapping("/requests/{matchId}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptRequest(
            @PathVariable int matchId,
            @RequestParam String lawyerId) {
        requestListService.acceptRequest(matchId, lawyerId);

        return ResponseEntity.ok(
                ApiResponse.success("의뢰를 수락했습니다.", null));
    }

    @PatchMapping("/requests/{matchId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectRequest(
            @PathVariable int matchId,
            @RequestBody RequestRejectDto requestDto) {
        requestListService.rejectRequest(matchId, requestDto);

        return ResponseEntity.ok(
                ApiResponse.success("의뢰를 거절했습니다.", null));
    }

    @PatchMapping("/requests/{matchId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelRequest(
            @PathVariable int matchId,
            @RequestParam String userId) {
        requestListService.cancelRequest(matchId, userId);

        return ResponseEntity.ok(
                ApiResponse.success("의뢰가 취소되었습니다.", null));
    }

    @GetMapping("/requests/{matchId}/files/{fileId}/download")
    public ResponseEntity<byte[]> downloadRequestFile(
            @PathVariable int matchId,
            @PathVariable int fileId) {
        DirectRequestFileDto file = requestListService.getRequestFile(matchId, fileId);

        byte[] bytes = requestListService.downloadRequestFile(matchId, fileId);

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(file.getOriginalName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(bytes.length)
                .body(bytes);
    }
}