package com.sanavi.backend.admin.matchboard.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.sanavi.backend.admin.matchboard.dto.AdminMatchBidResponseDto;
import com.sanavi.backend.admin.matchboard.dto.AdminMatchBoardActionHistory;
import com.sanavi.backend.admin.matchboard.dto.AdminMatchBoardListRequestDto;
import com.sanavi.backend.admin.matchboard.dto.AdminMatchBoardTargetInfo;
import com.sanavi.backend.admin.matchboard.dto.AdminMatchPostResponseDto;

@Mapper
public interface AdminMatchBoardMapper {

    int countPosts(AdminMatchBoardListRequestDto request);

    List<AdminMatchPostResponseDto> selectPosts(AdminMatchBoardListRequestDto request);

    int countBids(AdminMatchBoardListRequestDto request);

    List<AdminMatchBidResponseDto> selectBids(AdminMatchBoardListRequestDto request);

    AdminMatchBoardTargetInfo selectPostTarget(@Param("matchId") int matchId);

    AdminMatchBoardTargetInfo selectBidTarget(@Param("bidId") int bidId);

    int closePost(@Param("matchId") int matchId);

    int softDeletePost(@Param("matchId") int matchId);

    int restorePost(@Param("matchId") int matchId);

    int softDeleteBid(@Param("bidId") int bidId);

    int restoreBid(@Param("bidId") int bidId);

    int insertActionHistory(AdminMatchBoardActionHistory history);
}