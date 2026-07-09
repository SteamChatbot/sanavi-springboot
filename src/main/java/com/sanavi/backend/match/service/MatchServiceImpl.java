package com.sanavi.backend.match.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.sanavi.backend.common.service.S3Service;
import com.sanavi.backend.common.dto.PageResponse;
import com.sanavi.backend.match.dto.MatchBidRequestDto;
import com.sanavi.backend.match.dto.MatchBidResponseDto;
import com.sanavi.backend.match.dto.MatchFileDto;
import com.sanavi.backend.match.dto.MatchListResponseDto;
import com.sanavi.backend.match.dto.MatchRequestDto;
import com.sanavi.backend.match.dto.MatchResponseDto;
import com.sanavi.backend.match.dto.MatchSearchDto;
import com.sanavi.backend.match.mapper.MatchBidMapper;
import com.sanavi.backend.match.mapper.MatchFileMapper;
import com.sanavi.backend.match.mapper.MatchMapper;

import com.sanavi.backend.common.annotation.LogAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 책임: MatchService 구현체 — 의뢰글·입찰·첨부파일 비즈니스 로직 처리
//       PDF 파일은 S3 "match/" 폴더에 업로드, file_path에 S3 URL 저장
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {

    private static final long MAX_TOTAL_FILE_SIZE = 50L * 1024 * 1024;
    private final MatchMapper matchMapper;
    private final MatchBidMapper matchBidMapper;
    private final MatchFileMapper matchFileMapper;
    private final S3Service s3Service;

    // Input: page, size, userId (null=전체 / 값=본인 필터)
    // status, preferredRegion, minPrice — 선택 필터 (null/0이면 조건 무시)
    // Output: PageResponse<MatchListResponseDto>
    @Override
    public PageResponse<MatchListResponseDto> getMatchList(
            int page, int size, String userId,
            String status, String preferredRegion, Integer minPrice) {
        MatchSearchDto search = MatchSearchDto.builder()
                .offset((page - 1) * size)
                .size(size)
                .userId(userId)
                .status(status)
                .preferredRegion(preferredRegion)
                .minPrice(minPrice)
                .build();

        List<MatchListResponseDto> contents = matchMapper.selectMatchList(search);
        int totalCount = matchMapper.selectMatchCount(search);

        return new PageResponse<>(contents, page, size, totalCount);
    }

    // Input: matchId (의뢰글 PK)
    // Output: MatchResponseDto — files 필드에 첨부파일 목록 세팅
    // 격리수준: REPEATABLE_READ (MariaDB 기본값) / readOnly
    // readOnly: 공유락만 사용 → 불필요한 배타락 방지, MyBatis 더티체킹 스킵
    // 두 쿼리(match + files)가 동일 스냅샷을 봄 → 사이에 다른 트랜잭션이 수정해도 일관된 결과
    @Override
    @Transactional(readOnly = true)
    public MatchResponseDto getMatchById(int matchId) {
        MatchResponseDto match = matchMapper.selectMatchById(matchId);

        if (match == null) {
            log.warn(
                    "action=MATCH_DETAIL_VIEW target_type=match target_id={} result=FAIL reason=MATCH_NOT_FOUND",
                    matchId);

            return null;
        }

        match.setFiles(matchFileMapper.selectFilesByMatchId(matchId));

        return match;
    }

    // Input: MatchRequestDto (의뢰글 데이터), pdf (MultipartFile)
    // Output: void
    // 책임: 의뢰글 INSERT → S3에 PDF 업로드 → match_file INSERT (단일 트랜잭션)
    @LogAction(action = "의뢰글 등록", domain = "MATCH", key = "'userId=' + #requestDto.userId")
    @Override
    @Transactional(rollbackFor = IOException.class)
    public void createMatch(MatchRequestDto requestDto, List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) {
            log.warn(
                    "action=MATCH_CREATE target_type=match request_user_id={} result=DENIED reason=EMPTY_FILE",
                    requestDto.getUserId());

            throw new IllegalArgumentException("첨부파일이 필요합니다.");
        }

        List<MultipartFile> validFiles = files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();

        if (validFiles.isEmpty()) {
            throw new IllegalArgumentException("첨부파일이 필요합니다.");
        }

        long totalSize = validFiles.stream()
                .mapToLong(MultipartFile::getSize)
                .sum();

        if (totalSize > MAX_TOTAL_FILE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "첨부파일 전체 용량은 최대 50MB까지 업로드할 수 있습니다.");
        }

        matchMapper.insertMatch(requestDto);
        int matchId = requestDto.getId();

        if (matchId <= 0) {
            log.error(
                    "action=MATCH_CREATE target_type=match request_user_id={} result=FAIL reason=MATCH_ID_NOT_GENERATED",
                    requestDto.getUserId());

            throw new IllegalStateException("의뢰글 등록에 실패했습니다.");
        }

        try {
            for (MultipartFile file : validFiles) {
                String originalName = file.getOriginalFilename();
                String savedName = UUID.randomUUID() + "_" + originalName;
                String s3Url = s3Service.upload(file, "match/" + matchId, savedName);

                MatchFileDto fileDto = new MatchFileDto();
                fileDto.setMatchId(matchId);
                fileDto.setOriginalName(originalName);
                fileDto.setSavedName(savedName);
                fileDto.setFilePath(s3Url);
                fileDto.setFileSize(file.getSize());
                fileDto.setFileType(getExtension(originalName));

                matchFileMapper.insertFile(fileDto);
            }

            log.info(
                    "action=MATCH_CREATE target_type=match target_id={} request_user_id={} file_count={} total_file_size={} result=SUCCESS",
                    matchId,
                    requestDto.getUserId(),
                    validFiles.size(),
                    totalSize);
        } catch (IOException e) {
            log.error(
                    "action=MATCH_CREATE target_type=match target_id={} request_user_id={} result=FAIL reason=S3_UPLOAD_FAILED exception={}",
                    matchId,
                    requestDto.getUserId(),
                    e.getClass().getSimpleName());

            throw e;
        }
    }

    @LogAction(action = "상태 변경", domain = "MATCH", key = "'matchId=' + #matchId + ' status=' + #status + ' by=' + #requestUserId")
    @Override
    @Transactional
    public void updateMatchStatus(int matchId, String status, String requestUserId) {
        if ("CANCELLED".equals(status)) {
            String owner = matchMapper.selectMatchOwner(matchId);

            if (owner == null) {
                log.warn(
                        "action=MATCH_STATUS_UPDATE target_type=match target_id={} status={} request_user_id={} result=FAIL reason=MATCH_NOT_FOUND",
                        matchId,
                        status,
                        requestUserId);

                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 의뢰글입니다.");
            }

            if (!owner.equals(requestUserId)) {
                log.warn(
                        "action=MATCH_STATUS_UPDATE target_type=match target_id={} status={} request_user_id={} result=DENIED reason=NOT_OWNER",
                        matchId,
                        status,
                        requestUserId);

                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 의뢰글만 취소할 수 있습니다.");
            }
        }

        matchMapper.updateMatchStatus(matchId, status);

        log.info(
                "action=MATCH_STATUS_UPDATE target_type=match target_id={} status={} request_user_id={} result=SUCCESS",
                matchId,
                status,
                requestUserId);
    }

    @LogAction(action = "의뢰글 삭제", domain = "MATCH", key = "'matchId=' + #matchId")
    @Override
    @Transactional
    public void deleteMatch(int matchId) {
        matchMapper.deleteMatch(matchId);
        matchFileMapper.deleteFilesByMatchId(matchId);

        log.info(
                "action=MATCH_DELETE target_type=match target_id={} result=SUCCESS",
                matchId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchBidResponseDto> getBidList(int matchId) {
        return matchBidMapper.selectBidListByMatchId(matchId);
    }

    @LogAction(action = "입찰 등록", domain = "MATCH", key = "'matchId=' + #requestDto.matchId + ' lawyerId=' + #requestDto.lawyerId")
    @Override
    @Transactional
    public void createBid(MatchBidRequestDto requestDto) {
        matchBidMapper.insertBid(requestDto);

        MatchResponseDto match = matchMapper.selectMatchById(requestDto.getMatchId());

        if (match != null && "OPEN".equals(match.getStatus())) {
            matchMapper.updateMatchStatus(requestDto.getMatchId(), "BIDDING");
        }

        log.info(
                "action=MATCH_BID_CREATE target_type=match target_id={} lawyer_id={} result=SUCCESS",
                requestDto.getMatchId(),
                requestDto.getLawyerId());
    }

    // 격리수준: REPEATABLE_READ (MariaDB 기본값)
    // 세 번의 UPDATE가 원자적으로 처리 — 중간 실패 시 전체 롤백
    // SERIALIZABLE이 아니므로 동시에 두 명이 같은 입찰을 확정하는 race condition 이론상 가능
    // → 실제로는 match 상태를 CLOSED로 먼저 바꾸는 쪽이 이기고, 나머지는 FORBIDDEN 처리
    @LogAction(action = "입찰 확정", domain = "MATCH", key = "'matchId=' + #matchId + ' bidId=' + #bidId + ' status=matched'")
    @Override
    @Transactional
    public void selectBid(int matchId, int bidId) {
        matchBidMapper.updateBidStatus(bidId, "SELECTED");
        matchBidMapper.rejectOtherBids(matchId, bidId);
        matchMapper.updateMatchStatus(matchId, "CLOSED");

        log.info(
                "action=MATCH_BID_SELECT target_type=match target_id={} bid_id={} result=SUCCESS",
                matchId,
                bidId);
    }

    @LogAction(action = "입찰 거절", domain = "MATCH", key = "'matchId=' + #matchId + ' bidId=' + #bidId + ' by=' + #userId")
    @Override
    @Transactional
    public void rejectBid(int matchId, int bidId, String userId) {
        String owner = matchMapper.selectMatchOwner(matchId);

        if (owner == null || !owner.equals(userId)) {
            log.warn(
                    "action=MATCH_BID_REJECT target_type=match target_id={} bid_id={} request_user_id={} result=DENIED reason=NOT_OWNER_OR_MATCH_NOT_FOUND",
                    matchId,
                    bidId,
                    userId);

            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 의뢰글에 대한 입찰만 거절할 수 있습니다.");
        }

        matchBidMapper.updateBidStatus(bidId, "REJECTED");

        log.info(
                "action=MATCH_BID_REJECT target_type=match target_id={} bid_id={} request_user_id={} result=SUCCESS",
                matchId,
                bidId,
                userId);
    }

    @LogAction(action = "입찰 취소", domain = "MATCH", key = "'bidId=' + #bidId + ' lawyerId=' + #lawyerId")
    @Override
    @Transactional
    public void cancelBid(int bidId, String lawyerId) {
        var bid = matchBidMapper.selectBidById(bidId);

        if (bid == null || !bid.getLawyerId().equals(lawyerId)) {
            log.warn(
                    "action=MATCH_BID_CANCEL target_type=match_bid target_id={} lawyer_id={} result=DENIED reason=NOT_OWNER_OR_BID_NOT_FOUND",
                    bidId,
                    lawyerId);

            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 입찰만 취소할 수 있습니다.");
        }

        matchBidMapper.deleteBid(bidId);

        log.info(
                "action=MATCH_BID_CANCEL target_type=match_bid target_id={} lawyer_id={} result=SUCCESS",
                bidId,
                lawyerId);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains("."))
            return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}