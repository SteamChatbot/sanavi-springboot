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
import com.sanavi.backend.match.mapper.MatchBidMapper;
import com.sanavi.backend.match.mapper.MatchFileMapper;
import com.sanavi.backend.match.mapper.MatchMapper;

import lombok.RequiredArgsConstructor;

// 책임: MatchService 구현체 — 의뢰글·입찰·첨부파일 비즈니스 로직 처리
//       PDF 파일은 S3 "match/" 폴더에 업로드, file_path에 S3 URL 저장
@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {

    private final MatchMapper matchMapper;
    private final MatchBidMapper matchBidMapper;
    private final MatchFileMapper matchFileMapper;
    private final S3Service s3Service;

    // Input:  page, size, userId (null = 전체 조회 / 값 있으면 해당 유저 필터)
    // Output: PageResponse<MatchListResponseDto>
    @Override
    public PageResponse<MatchListResponseDto> getMatchList(int page, int size, String userId) {
        int offset = (page - 1) * size;
        List<MatchListResponseDto> contents = matchMapper.selectMatchList(offset, size, userId);
        int totalCount = matchMapper.selectMatchCount(userId);
        return new PageResponse<>(contents, page, size, totalCount);
    }

    // Input:  matchId (의뢰글 PK)
    // Output: MatchResponseDto — files 필드에 첨부파일 목록 세팅
    @Override
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

    @Override
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

    @Override
    public void deleteMatch(int matchId) {
        matchMapper.deleteMatch(matchId);
        matchFileMapper.deleteFilesByMatchId(matchId);
    }

    @Override
    public List<MatchBidResponseDto> getBidList(int matchId) {
        return matchBidMapper.selectBidListByMatchId(matchId);
    }

    @Override
    @Transactional
    public void createBid(MatchBidRequestDto requestDto) {
        matchBidMapper.insertBid(requestDto);

        MatchResponseDto match = matchMapper.selectMatchById(requestDto.getMatchId());
        if (match != null && "OPEN".equals(match.getStatus())) {
            matchMapper.updateMatchStatus(requestDto.getMatchId(), "BIDDING");
        }
    }

    @Override
    @Transactional
    public void selectBid(int matchId, int bidId) {
        matchBidMapper.updateBidStatus(bidId, "SELECTED");
        matchBidMapper.rejectOtherBids(matchId, bidId);
        matchMapper.updateMatchStatus(matchId, "CLOSED");
    }

    @Override
    public void rejectBid(int matchId, int bidId, String userId) {
        String owner = matchMapper.selectMatchOwner(matchId);
        if (owner == null || !owner.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 의뢰글에 대한 입찰만 거절할 수 있습니다.");
        }
        matchBidMapper.updateBidStatus(bidId, "REJECTED");
    }

    @Override
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
