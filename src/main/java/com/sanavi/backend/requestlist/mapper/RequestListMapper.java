package com.sanavi.backend.requestlist.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.sanavi.backend.requestlist.dto.DirectRequestCreateDto;
import com.sanavi.backend.requestlist.dto.DirectRequestFileDto;
import com.sanavi.backend.requestlist.dto.DirectRequestFileResponseDto;
import com.sanavi.backend.requestlist.dto.DirectRequestResponseDto;
import com.sanavi.backend.requestlist.dto.LawyerDetailResponseDto;
import com.sanavi.backend.requestlist.dto.LawyerListResponseDto;
import com.sanavi.backend.requestlist.dto.ReceivedRequestResponseDto;
import com.sanavi.backend.requestlist.dto.SentRequestResponseDto;

@Mapper
public interface RequestListMapper {

    List<LawyerListResponseDto> selectLawyerList();

    LawyerDetailResponseDto selectLawyerDetail(@Param("lawyerId") String lawyerId);

    int insertDirectMatch(DirectRequestCreateDto requestDto);

    int insertDirectBid(
            @Param("matchId") int matchId,
            @Param("request") DirectRequestCreateDto requestDto);

    int insertDirectRequestFile(DirectRequestFileDto fileDto);

    List<DirectRequestFileResponseDto> selectFilesByMatchId(@Param("matchId") int matchId);

    DirectRequestResponseDto selectDirectRequestDetail(@Param("matchId") int matchId);

    List<SentRequestResponseDto> selectSentRequests(@Param("userId") String userId);

    List<ReceivedRequestResponseDto> selectReceivedRequests(@Param("lawyerId") String lawyerId);

    DirectRequestFileDto selectRequestFileById(
            @Param("matchId") int matchId,
            @Param("fileId") int fileId);

    int acceptDirectBid(
            @Param("matchId") int matchId,
            @Param("lawyerId") String lawyerId);

    int rejectDirectBid(
            @Param("matchId") int matchId,
            @Param("lawyerId") String lawyerId,
            @Param("message") String message);

    int updateDirectMatchStatus(
            @Param("matchId") int matchId,
            @Param("status") String status);

    int cancelDirectMatch(
            @Param("matchId") int matchId,
            @Param("userId") String userId);

    int cancelDirectBidByMatchId(@Param("matchId") int matchId);
}