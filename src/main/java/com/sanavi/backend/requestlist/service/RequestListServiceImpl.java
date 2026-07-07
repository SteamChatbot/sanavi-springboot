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
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
            log.warn(
                    "action=LAWYER_DETAIL_READ target_type=lawyer target_user_id={} result=FAIL reason=LAWYER_NOT_FOUND",
                    lawyerId);

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
            log.warn(
                    "action=DIRECT_REQUEST_CREATE target_type=match request_user_id={} lawyer_id={} result=DENIED reason=LAWYER_NOT_FOUND",
                    requestDto.getUserId(),
                    requestDto.getLawyerId());

            throw new IllegalArgumentException("존재하지 않는 변호사입니다.");
        }

        requestListMapper.insertDirectMatch(requestDto);

        int matchId = requestDto.getMatchId();

        if (matchId <= 0) {
            log.error(
                    "action=DIRECT_REQUEST_CREATE target_type=match request_user_id={} lawyer_id={} result=FAIL reason=MATCH_ID_NOT_GENERATED",
                    requestDto.getUserId(),
                    requestDto.getLawyerId());

            throw new IllegalStateException("의뢰글 생성에 실패했습니다.");
        }

        requestListMapper.insertDirectBid(matchId, requestDto);

        int uploadedFileCount = 0;

        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    continue;
                }

                DirectRequestFileDto uploadedFile = requestListFileService.uploadRequestFile(matchId, file);

                requestListMapper.insertDirectRequestFile(uploadedFile);
                uploadedFileCount++;
            }
        }

        log.info(
                "action=DIRECT_REQUEST_CREATE target_type=match target_id={} request_user_id={} lawyer_id={} file_count={} result=SUCCESS",
                matchId,
                requestDto.getUserId(),
                requestDto.getLawyerId(),
                uploadedFileCount);

        return matchId;
    }

    private void validateCreateRequest(DirectRequestCreateDto requestDto) {
        if (requestDto.getUserId() == null || requestDto.getUserId().isBlank()) {
            log.warn(
                    "action=DIRECT_REQUEST_CREATE target_type=match result=DENIED reason=MISSING_USER_ID");

            throw new IllegalArgumentException("의뢰자 아이디가 필요합니다.");
        }

        if (requestDto.getLawyerId() == null || requestDto.getLawyerId().isBlank()) {
            log.warn(
                    "action=DIRECT_REQUEST_CREATE target_type=match request_user_id={} result=DENIED reason=MISSING_LAWYER_ID",
                    requestDto.getUserId());

            throw new IllegalArgumentException("변호사 아이디가 필요합니다.");
        }

        if (requestDto.getTitle() == null || requestDto.getTitle().isBlank()) {
            log.warn(
                    "action=DIRECT_REQUEST_CREATE target_type=match request_user_id={} lawyer_id={} result=DENIED reason=MISSING_TITLE",
                    requestDto.getUserId(),
                    requestDto.getLawyerId());

            throw new IllegalArgumentException("제목을 입력해 주세요.");
        }

        if (requestDto.getContent() == null || requestDto.getContent().isBlank()) {
            log.warn(
                    "action=DIRECT_REQUEST_CREATE target_type=match request_user_id={} lawyer_id={} result=DENIED reason=MISSING_CONTENT",
                    requestDto.getUserId(),
                    requestDto.getLawyerId());

            throw new IllegalArgumentException("의뢰 내용을 입력해 주세요.");
        }

        if (requestDto.getPrice() == null || requestDto.getPrice() < 0) {
            log.warn(
                    "action=DIRECT_REQUEST_CREATE target_type=match request_user_id={} lawyer_id={} result=DENIED reason=INVALID_PRICE",
                    requestDto.getUserId(),
                    requestDto.getLawyerId());

            throw new IllegalArgumentException("희망 보수를 올바르게 입력해 주세요.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DirectRequestResponseDto getDirectRequestDetail(int matchId) {
        DirectRequestResponseDto request = requestListMapper.selectDirectRequestDetail(matchId);

        if (request == null) {
            log.warn(
                    "action=DIRECT_REQUEST_DETAIL_READ target_type=match target_id={} result=FAIL reason=REQUEST_NOT_FOUND",
                    matchId);

            throw new IllegalArgumentException("의뢰 정보를 찾을 수 없습니다.");
        }

        request.setFiles(requestListMapper.selectFilesByMatchId(matchId));

        return request;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SentRequestResponseDto> getSentRequests(String userId) {
        if (userId == null || userId.isBlank()) {
            log.warn(
                    "action=DIRECT_REQUEST_SENT_LIST_READ target_type=match result=DENIED reason=MISSING_USER_ID");

            throw new IllegalArgumentException("사용자 아이디가 필요합니다.");
        }

        return requestListMapper.selectSentRequests(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReceivedRequestResponseDto> getReceivedRequests(String lawyerId) {
        if (lawyerId == null || lawyerId.isBlank()) {
            log.warn(
                    "action=DIRECT_REQUEST_RECEIVED_LIST_READ target_type=match result=DENIED reason=MISSING_LAWYER_ID");

            throw new IllegalArgumentException("변호사 아이디가 필요합니다.");
        }

        return requestListMapper.selectReceivedRequests(lawyerId);
    }

    @Override
    @Transactional
    public void acceptRequest(int matchId, String lawyerId) {
        if (lawyerId == null || lawyerId.isBlank()) {
            log.warn(
                    "action=DIRECT_REQUEST_ACCEPT target_type=match target_id={} result=DENIED reason=MISSING_LAWYER_ID",
                    matchId);

            throw new IllegalArgumentException("변호사 아이디가 필요합니다.");
        }

        int bidResult = requestListMapper.acceptDirectBid(matchId, lawyerId);

        if (bidResult != 1) {
            log.warn(
                    "action=DIRECT_REQUEST_ACCEPT target_type=match target_id={} lawyer_id={} result=DENIED reason=NOT_ACCEPTABLE",
                    matchId,
                    lawyerId);

            throw new IllegalStateException("수락 가능한 의뢰가 아닙니다.");
        }

        requestListMapper.updateDirectMatchStatus(matchId, "ACCEPTED");

        log.info(
                "action=DIRECT_REQUEST_ACCEPT target_type=match target_id={} lawyer_id={} result=SUCCESS",
                matchId,
                lawyerId);
    }

    @Override
    @Transactional
    public void rejectRequest(int matchId, RequestRejectDto requestDto) {
        if (requestDto.getLawyerId() == null || requestDto.getLawyerId().isBlank()) {
            log.warn(
                    "action=DIRECT_REQUEST_REJECT target_type=match target_id={} result=DENIED reason=MISSING_LAWYER_ID",
                    matchId);

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
            log.warn(
                    "action=DIRECT_REQUEST_REJECT target_type=match target_id={} lawyer_id={} result=DENIED reason=NOT_REJECTABLE",
                    matchId,
                    requestDto.getLawyerId());

            throw new IllegalStateException("거절 가능한 의뢰가 아닙니다.");
        }

        requestListMapper.updateDirectMatchStatus(matchId, "REJECTED");

        log.info(
                "action=DIRECT_REQUEST_REJECT target_type=match target_id={} lawyer_id={} result=SUCCESS",
                matchId,
                requestDto.getLawyerId());
    }

    @Override
    @Transactional
    public void cancelRequest(int matchId, String userId) {
        if (userId == null || userId.isBlank()) {
            log.warn(
                    "action=DIRECT_REQUEST_CANCEL target_type=match target_id={} result=DENIED reason=MISSING_USER_ID",
                    matchId);

            throw new IllegalArgumentException("사용자 아이디가 필요합니다.");
        }

        int matchResult = requestListMapper.cancelDirectMatch(matchId, userId);

        if (matchResult != 1) {
            log.warn(
                    "action=DIRECT_REQUEST_CANCEL target_type=match target_id={} request_user_id={} result=DENIED reason=NOT_CANCELABLE",
                    matchId,
                    userId);

            throw new IllegalStateException("취소 가능한 의뢰가 아닙니다.");
        }

        requestListMapper.cancelDirectBidByMatchId(matchId);

        log.info(
                "action=DIRECT_REQUEST_CANCEL target_type=match target_id={} request_user_id={} result=SUCCESS",
                matchId,
                userId);
    }

    @Override
    @Transactional(readOnly = true)
    public DirectRequestFileDto getRequestFile(int matchId, int fileId) {
        DirectRequestFileDto file = requestListMapper.selectRequestFileById(matchId, fileId);

        if (file == null) {
            log.warn(
                    "action=DIRECT_REQUEST_FILE_READ target_type=match_file target_id={} match_id={} result=FAIL reason=FILE_NOT_FOUND",
                    fileId,
                    matchId);

            throw new IllegalArgumentException("첨부파일을 찾을 수 없습니다.");
        }

        return file;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadRequestFile(int matchId, int fileId) {
        DirectRequestFileDto file = getRequestFile(matchId, fileId);

        byte[] data = requestListFileService.download(file.getFilePath());

        log.info(
                "action=DIRECT_REQUEST_FILE_DOWNLOAD target_type=match_file target_id={} match_id={} file_size={} result=SUCCESS",
                fileId,
                matchId,
                data.length);

        return data;
    }
}