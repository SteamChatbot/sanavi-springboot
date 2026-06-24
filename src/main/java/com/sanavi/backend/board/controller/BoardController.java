package com.sanavi.backend.board.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sanavi.backend.board.dto.BoardFileDto;
import com.sanavi.backend.board.dto.BoardFileResponseDto;
import com.sanavi.backend.board.dto.BoardListResponseDto;
import com.sanavi.backend.board.dto.BoardRequestDto;
import com.sanavi.backend.board.dto.BoardResponseDto;
import com.sanavi.backend.board.service.BoardService;
import com.sanavi.backend.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService boardService;

    @GetMapping
    public ResponseEntity<ApiResponse<BoardListResponseDto>> getBoardList(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "searchType", defaultValue = "all") String searchType) {
        BoardListResponseDto response = boardService.getBoardList(page, size, keyword, searchType);
        return ResponseEntity.ok(ApiResponse.success("게시글 목록 조회 성공", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BoardResponseDto>> getBoardById(
            @PathVariable("id") int id) {
        BoardResponseDto response = boardService.getBoardById(id);

        return ResponseEntity.ok(
                ApiResponse.success("게시글 상세 조회 성공", response));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Integer>>> createBoard(
            @ModelAttribute BoardRequestDto requestDto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        int boardId = boardService.createBoard(requestDto, files);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "게시글이 등록되었습니다.",
                        Map.of("boardId", boardId)));
    }

    @PatchMapping("/{boardId}")
    public ResponseEntity<ApiResponse<Void>> updateBoard(
            @PathVariable("boardId") int boardId,
            @RequestBody BoardRequestDto requestDto) {
        boardService.updateBoard(boardId, requestDto);
        return ResponseEntity.ok(ApiResponse.success("게시글이 수정되었습니다.", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoard(@PathVariable("id") int id) {
        boardService.deleteBoard(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{boardId}/files/{fileId}/download")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable("boardId") int boardId,
            @PathVariable("fileId") int fileId) {
        BoardFileDto file = boardService.getBoardFile(boardId, fileId);
        byte[] bytes = boardService.downloadBoardFile(boardId, fileId);

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(file.getOriginalName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentLength(bytes.length)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @PostMapping(value = "/{boardId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<BoardFileResponseDto>>> addBoardFiles(
            @PathVariable("boardId") int boardId,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        List<BoardFileResponseDto> response = boardService.addBoardFiles(boardId, files);

        return ResponseEntity.ok(
                ApiResponse.success("첨부파일이 추가되었습니다.", response));
    }

    @DeleteMapping("/{boardId}/files/{fileId}")
    public ResponseEntity<ApiResponse<Void>> deleteBoardFile(
            @PathVariable("boardId") int boardId,
            @PathVariable("fileId") int fileId) {
        boardService.deleteBoardFile(boardId, fileId);

        return ResponseEntity.ok(
                ApiResponse.success("첨부파일이 삭제 처리되었습니다.", null));
    }
}