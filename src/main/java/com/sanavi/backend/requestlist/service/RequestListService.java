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

public interface RequestListService {

    List<LawyerListResponseDto> getLawyerList();

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