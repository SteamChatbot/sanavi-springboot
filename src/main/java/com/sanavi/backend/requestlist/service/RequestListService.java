package com.sanavi.backend.requestlist.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.sanavi.backend.requestlist.dto.DirectRequestCreateDto;
import com.sanavi.backend.requestlist.dto.DirectRequestFileDto;
import com.sanavi.backend.requestlist.dto.DirectRequestResponseDto;
import com.sanavi.backend.requestlist.dto.LawyerDetailResponseDto;
import com.sanavi.backend.requestlist.dto.LawyerListResponseDto;
import com.sanavi.backend.requestlist.dto.ReceivedRequestResponseDto;
import com.sanavi.backend.requestlist.dto.RequestRejectDto;
import com.sanavi.backend.requestlist.dto.SentRequestResponseDto;

// 책임: 변호사 직접 의뢰 비즈니스 로직 계약 정의
public interface RequestListService {

    // Input:  specialty (null=전체 / 전문분야명 LIKE), sido (null=전체 / 시도명 완전일치)
    // Output: List<LawyerListResponseDto>
    // 책임:   LawyerSearchDto 조립 후 Mapper 위임
    List<LawyerListResponseDto> getLawyerList(String specialty, String sido);

    LawyerDetailResponseDto getLawyerDetail(String lawyerId);

    int createDirectRequest(
            DirectRequestCreateDto requestDto,
            List<MultipartFile> files);

    DirectRequestResponseDto getDirectRequestDetail(int matchId);

    List<SentRequestResponseDto> getSentRequests(String userId);

    List<ReceivedRequestResponseDto> getReceivedRequests(String lawyerId);

    void acceptRequest(int matchId, String lawyerId);

    void rejectRequest(int matchId, RequestRejectDto requestDto);

    void cancelRequest(int matchId, String userId);

    DirectRequestFileDto getRequestFile(int matchId, int fileId);

    byte[] downloadRequestFile(int matchId, int fileId);
}