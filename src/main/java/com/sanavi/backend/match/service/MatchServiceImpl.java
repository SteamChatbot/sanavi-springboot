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

    private final MatchMapper matchMapper;
    private final MatchBidMapper matchBidMapper;
    private final MatchFileMapper matchFileMapper;
    private final S3Service s3Service;

    // Input:  page, size, userId (null=전체 / 값=본인 필터)
    //         status, preferredRegion, minPrice — 선택 필터 (null/0이면 조건 무시)
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

    // Input:  matchId (의뢰글 PK)
    // Output: MatchResponseDto — files 필드에 첨부파일 목록 세팅
    // 격리수준: REPEATABLE_READ (MariaDB 기본값) / readOnly
    // readOnly: 공유락만 사용 → 불필요한 배타락 방지, MyBatis 더티체킹 스킵
    // 두 쿼리(match + files)가 동일 스냅샷을 봄 → 사이에 다른 트랜잭션이 수정해도 일관된 결과
    @Override
    @Transactional(readOnly = true)
    public MatchResponseDto getMatchById(int matchId) {
        MatchResponseDto match = matchMapper.selectMatchById(matchId);
        if (match != null) {
            match.setFiles(matchFileMapper.selectFilesByMatchId(matchId));
        }
        return match;
    }

    // Input:  MatchRequestDto (의뢰글 데이터), pdf (MultipartFile)
    // Output: void
    // 책임:   의뢰글 INSERT → S3에 PDF 업로드 → match_file INSERT (단일 트랜잭션)
    @LogAction(action = "의뢰글 등록", domain = "MATCH",
            key = "'userId=' + #requestDto.userId + ' title=' + #requestDto.title")
    @Override
    @Transactional
    public void createMatch(MatchRequestDto requestDto, MultipartFile pdf) throws IOException {
        matchMapper.insertMatch(requestDto);
        int matchId = requestDto.getId();

        String savedName = UUID.randomUUID() + "_" + pdf.getOriginalFilename();
        String s3Url = s3Service.upload(pdf, "match/" + matchId, savedName);

        MatchFileDto fileDto = new MatchFileDto();
        fileDto.setMatchId(matchId);
        fileDto.setOriginalName(pdf.getOriginalFilename());
        fileDto.setSavedName(savedName);
        fileDto.setFilePath(s3Url);
        fileDto.setFileSize(pdf.getSize());
        fileDto.setFileType(getExtension(pdf.getOriginalFilename()));
        matchFileMapper.insertFile(fileDto);
    }

    @LogAction(action = "상태 변경", domain = "MATCH",
            key = "'matchId=' + #matchId + ' → ' + #status + ' by=' + #requestUserId")
    @Override
    @Transactional
    public void updateMatchStatus(int matchId, String status, String requestUserId) {
        if ("CANCELLED".equals(status)) {
            String owner = matchMapper.selectMatchOwner(matchId);
            if (owner == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 의뢰글입니다.");
            }
            if (!owner.equals(requestUserId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 의뢰글만 취소할 수 있습니다.");
            }
        }
        matchMapper.updateMatchStatus(matchId, status);
    }

    @LogAction(action = "의뢰글 삭제", domain = "MATCH", key = "'matchId=' + #matchId")
    @Override
    @Transactional
    public void deleteMatch(int matchId) {
        matchMapper.deleteMatch(matchId);
        matchFileMapper.deleteFilesByMatchId(matchId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchBidResponseDto> getBidList(int matchId) {
        return matchBidMapper.selectBidListByMatchId(matchId);
    }

    @LogAction(action = "입찰 등록", domain = "MATCH",
            key = "'matchId=' + #requestDto.matchId + ' lawyerId=' + #requestDto.lawyerId")
    @Override
    @Transactional
    public void createBid(MatchBidRequestDto requestDto) {
        matchBidMapper.insertBid(requestDto);

        MatchResponseDto match = matchMapper.selectMatchById(requestDto.getMatchId());
        if (match != null && "OPEN".equals(match.getStatus())) {
            matchMapper.updateMatchStatus(requestDto.getMatchId(), "BIDDING");
        }
    }

    // 격리수준: REPEATABLE_READ (MariaDB 기본값)
    // 세 번의 UPDATE가 원자적으로 처리 — 중간 실패 시 전체 롤백
    // SERIALIZABLE이 아니므로 동시에 두 명이 같은 입찰을 확정하는 race condition 이론상 가능
    // → 실제로는 match 상태를 CLOSED로 먼저 바꾸는 쪽이 이기고, 나머지는 FORBIDDEN 처리
    @LogAction(action = "입찰 확정", domain = "MATCH",
            key = "'matchId=' + #matchId + ' bidId=' + #bidId + ' → 매칭 완료'")
    @Override
    @Transactional
    public void selectBid(int matchId, int bidId) {
        matchBidMapper.updateBidStatus(bidId, "SELECTED");
        matchBidMapper.rejectOtherBids(matchId, bidId);
        matchMapper.updateMatchStatus(matchId, "CLOSED");
    }

    @LogAction(action = "입찰 거절", domain = "MATCH",
            key = "'matchId=' + #matchId + ' bidId=' + #bidId + ' by=' + #userId")
    @Override
    @Transactional
    public void rejectBid(int matchId, int bidId, String userId) {
        String owner = matchMapper.selectMatchOwner(matchId);
        if (owner == null || !owner.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 의뢰글에 대한 입찰만 거절할 수 있습니다.");
        }
        matchBidMapper.updateBidStatus(bidId, "REJECTED");
    }

    @LogAction(action = "입찰 취소", domain = "MATCH",
            key = "'bidId=' + #bidId + ' lawyerId=' + #lawyerId")
    @Override
    @Transactional
    public void cancelBid(int bidId, String lawyerId) {
        var bid = matchBidMapper.selectBidById(bidId);
        if (bid == null || !bid.getLawyerId().equals(lawyerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 입찰만 취소할 수 있습니다.");
        }
        matchBidMapper.deleteBid(bidId);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
