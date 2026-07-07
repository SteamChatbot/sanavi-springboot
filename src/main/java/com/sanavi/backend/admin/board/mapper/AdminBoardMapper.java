package com.sanavi.backend.admin.board.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.sanavi.backend.admin.board.dto.AdminBoardActionHistory;
import com.sanavi.backend.admin.board.dto.AdminBoardCommentResponseDto;
import com.sanavi.backend.admin.board.dto.AdminBoardListRequestDto;
import com.sanavi.backend.admin.board.dto.AdminBoardPostResponseDto;
import com.sanavi.backend.admin.board.dto.AdminBoardTargetInfo;

@Mapper
public interface AdminBoardMapper {

    int countPosts(AdminBoardListRequestDto request);

    List<AdminBoardPostResponseDto> selectPosts(AdminBoardListRequestDto request);

    int countComments(AdminBoardListRequestDto request);

    List<AdminBoardCommentResponseDto> selectComments(AdminBoardListRequestDto request);

    AdminBoardTargetInfo selectPostTarget(@Param("boardId") int boardId);

    AdminBoardTargetInfo selectCommentTarget(@Param("commentId") int commentId);

    int softDeletePost(@Param("boardId") int boardId);

    int restorePost(@Param("boardId") int boardId);

    int softDeleteComment(@Param("commentId") int commentId);

    int restoreComment(@Param("commentId") int commentId);

    int insertActionHistory(AdminBoardActionHistory history);
}