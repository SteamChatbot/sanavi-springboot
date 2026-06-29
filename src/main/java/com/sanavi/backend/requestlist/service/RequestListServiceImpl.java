package com.sanavi.backend.requestlist.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.sanavi.backend.requestlist.dto.DirectRequestCreateDto;
import com.sanavi.backend.requestlist.dto.DirectRequestFileDto;
import com.sanavi.backend.requestlist.dto.DirectRequestResponseDto;
import com.sanavi.backend.requestlist.dto.LawyerDetailResponseDto;
import com.sanavi.backend.requestlist.dto.LawyerListResponseDto;
import com.sanavi.backend.requestlist.dto.LawyerSearchDto;
import com.sanavi.backend.requestlist.dto.ReceivedRequestResponseDto;
import com.sanavi.backend.requestlist.dto.RequestRejectDto;
import com.sanavi.backend.requestlist.dto.SentRequestResponseDto;
import com.sanavi.backend.requestlist.mapper.RequestListMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RequestListServiceImpl implements RequestListService {

    private final RequestListMapper requestListMapper;
    private final RequestListFileService requestListFileService;

    @Override
    @Transactional(readOnly = true)
    public List<LawyerListResponseDto> getLawyerList(String specialty, String sido) {
        return requestListMapper.selectLawyerList(
                LawyerSearchDto.builder().specialty(specialty).sido(sido).build());
    }

    @Override
    @Transactional(readOnly = true)
    public LawyerDetailResponseDto getLawyerDetail(String lawyerId) {
        LawyerDetailResponseDto lawyer = requestListMapper.selectLawyerDetail(lawyerId);

        if (lawyer == null) {
            throw new IllegalArgumentException("변호사 정보를 찾을 수 없습니다.");
        }

        return lawyer;
    }

    @Override
    @Transactional
    public int createDirectRequest(
            DirectRequestCreateDto requestDto,
            List<MultipartFile> files) {
        validateCreateRequest(requestDto);

        LawyerDetailResponseDto lawyer = requestListMapper.selectLawyerDetail(requestDto.getLawyerId());

        if (lawyer == null) {
            throw new IllegalArgumentException("존재하지 않는 변호사입니다.");
        }

        requestListMapper.insertDirectMatch(requestDto);

        int matchId = requestDto.getMatchId();

        if (matchId <= 0) {
            throw new IllegalStateException("의뢰글 생성에 실패했습니다.");
        }

        requestListMapper.insertDirectBid(matchId, requestDto);

        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    continue;
                }

                DirectRequestFileDto uploadedFile = requestListFileService.uploadRequestFile(matchId, file);

                requestListMapper.insertDirectRequestFile(uploadedFile);
            }
        }

        return matchId;
    }

    private void validateCreateRequest(DirectRequestCreateDto requestDto) {
        if (requestDto.getUserId() == null || requestDto.getUserId().isBlank()) {
            throw new IllegalArgumentException("의뢰자 아이디가 필요합니다.");
        }

        if (requestDto.getLawyerId() == null || requestDto.getLawyerId().isBlank()) {
            throw new IllegalArgumentException("변호사 아이디가 필요합니다.");
        }

        if (requestDto.getTitle() == null || requestDto.getTitle().isBlank()) {
            throw new IllegalArgumentException("제목을 입력해 주세요.");
        }

        if (requestDto.getContent() == null || requestDto.getContent().isBlank()) {
            throw new IllegalArgumentException("의뢰 내용을 입력해 주세요.");
        }

        if (requestDto.getPrice() == null || requestDto.getPrice() < 0) {
            throw new IllegalArgumentException("희망 보수를 올바르게 입력해 주세요.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DirectRequestResponseDto getDirectRequestDetail(int matchId) {
        DirectRequestResponseDto request = requestListMapper.selectDirectRequestDetail(matchId);

        if (request == null) {
            throw new IllegalArgumentException("의뢰 정보를 찾을 수 없습니다.");
        }

        request.setFiles(requestListMapper.selectFilesByMatchId(matchId));

        return request;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SentRequestResponseDto> getSentRequests(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("사용자 아이디가 필요합니다.");
        }

        return requestListMapper.selectSentRequests(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReceivedRequestResponseDto> getReceivedRequests(String lawyerId) {
        if (lawyerId == null || lawyerId.isBlank()) {
            throw new IllegalArgumentException("변호사 아이디가 필요합니다.");
        }

        return requestListMapper.selectReceivedRequests(lawyerId);
    }

    @Override
    @Transactional
    public void acceptRequest(int matchId, String lawyerId) {
        if (lawyerId == null || lawyerId.isBlank()) {
            throw new IllegalArgumentException("변호사 아이디가 필요합니다.");
        }

        int bidResult = requestListMapper.acceptDirectBid(matchId, lawyerId);

        if (bidResult != 1) {
            throw new IllegalStateException("수락 가능한 의뢰가 아닙니다.");
        }

        requestListMapper.updateDirectMatchStatus(matchId, "ACCEPTED");
    }

    @Override
    @Transactional
    public void rejectRequest(int matchId, RequestRejectDto requestDto) {
        if (requestDto.getLawyerId() == null || requestDto.getLawyerId().isBlank()) {
            throw new IllegalArgumentException("변호사 아이디가 필요합니다.");
        }

        String message = requestDto.getMessage();

        if (message == null) {
            message = "";
        }

        int bidResult = requestListMapper.rejectDirectBid(
                matchId,
                requestDto.getLawyerId(),
                message);

        if (bidResult != 1) {
            throw new IllegalStateException("거절 가능한 의뢰가 아닙니다.");
        }

        requestListMapper.updateDirectMatchStatus(matchId, "REJECTED");
    }

    @Override
    @Transactional
    public void cancelRequest(int matchId, String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("사용자 아이디가 필요합니다.");
        }

        int matchResult = requestListMapper.cancelDirectMatch(matchId, userId);

        if (matchResult != 1) {
            throw new IllegalStateException("취소 가능한 의뢰가 아닙니다.");
        }

        requestListMapper.cancelDirectBidByMatchId(matchId);
    }

    @Override
    @Transactional(readOnly = true)
    public DirectRequestFileDto getRequestFile(int matchId, int fileId) {
        DirectRequestFileDto file = requestListMapper.selectRequestFileById(matchId, fileId);

        if (file == null) {
            throw new IllegalArgumentException("첨부파일을 찾을 수 없습니다.");
        }

        return file;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadRequestFile(int matchId, int fileId) {
        DirectRequestFileDto file = getRequestFile(matchId, fileId);

        return requestListFileService.download(file.getFilePath());
    }
}