package com.sanavi.backend.match.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.sanavi.backend.match.dto.MatchFileDto;

// 책임: match_file 테이블 CRUD MyBatis 매퍼
//       MatchFileMapper.xml (mapper/match/) 와 바인딩
@Mapper
public interface MatchFileMapper {

    // Input:  matchId (의뢰글 PK)
    // Output: List<MatchFileDto> — 해당 의뢰글의 유효 첨부파일 목록
    // 책임:   의뢰글 상세 조회 시 첨부파일 목록 세팅용
    MatchFileDto selectFileById(@Param("fileId") int fileId);

    List<MatchFileDto> selectFilesByMatchId(@Param("matchId") int matchId);

    // Input:  MatchFileDto (matchId·originalName·savedName·filePath·fileSize·fileType)
    // Output: int — 영향 받은 행 수 (useGeneratedKeys로 fileDto.fileId에 생성 PK 주입)
    // 책임:   파일 메타데이터 INSERT (실제 파일 저장은 MatchServiceImpl.saveFile에서 처리)
    int insertFile(MatchFileDto fileDto);

    // Input:  matchId
    // Output: int — 영향 받은 행 수
    // 책임:   의뢰글 삭제 시 연관 첨부파일 전체 논리 삭제
    int deleteFilesByMatchId(@Param("matchId") int matchId);

    // Input:  fileId (파일 PK)
    // Output: int — 영향 받은 행 수
    // 책임:   파일 단건 논리 삭제
    int deleteFileById(@Param("fileId") int fileId);
}
