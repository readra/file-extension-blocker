package com.flow.blocker.controller;

import com.flow.blocker.dto.ApiResponse;
import com.flow.blocker.dto.ExtensionRequest;
import com.flow.blocker.dto.ExtensionResponse;
import com.flow.blocker.service.ExtensionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/extensions")
@RequiredArgsConstructor
@Tag(name = "Extension API", description = "파일 확장자 관리 API")
public class ExtensionApiController {

    private final ExtensionService extensionService;

    /**
     * 고정 확장자 전체 조회
     */
    @GetMapping("/fixed")
    @Operation(summary = "고정 확장자 목록 조회", description = "시스템에 등록된 모든 고정 확장자 목록을 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = ExtensionResponse.class)))
    })
    public ResponseEntity<ApiResponse<List<ExtensionResponse>>> getFixedExtensions() {
        List<ExtensionResponse> extensions = extensionService.getAllFixedExtensions();
        return ResponseEntity.ok(ApiResponse.success(extensions));
    }

    /**
     * 고정 확장자 체크 상태 변경
     */
    @PatchMapping("/fixed/{extension}")
    @Operation(summary = "고정 확장자 상태 변경", description = "고정 확장자의 차단 활성화 상태를 변경합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "확장자를 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<ExtensionResponse>> updateFixedExtension(
            @Parameter(description = "확장자명", example = "exe") @PathVariable String extension,
            @Parameter(description = "체크 상태") @RequestBody Map<String, Boolean> request) {
        
        boolean checked = request.getOrDefault("checked", false);
        ExtensionResponse response = extensionService.updateFixedExtensionCheck(extension, checked);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 커스텀 확장자 전체 조회
     */
    @GetMapping("/custom")
    @Operation(summary = "커스텀 확장자 목록 조회", description = "등록된 모든 커스텀 확장자 목록을 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<ApiResponse<List<ExtensionResponse>>> getCustomExtensions() {
        List<ExtensionResponse> extensions = extensionService.getAllCustomExtensions();
        return ResponseEntity.ok(ApiResponse.success(extensions));
    }

    /**
     * 커스텀 확장자 추가
     */
    @PostMapping("/custom")
    @Operation(summary = "커스텀 확장자 추가", description = "새로운 커스텀 확장자를 추가합니다. (최대 200개, 20자 제한)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "추가 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (중복, 제한 초과 등)")
    })
    public ResponseEntity<ApiResponse<ExtensionResponse>> addCustomExtension(
            @Valid @RequestBody ExtensionRequest request) {
        
        ExtensionResponse response = extensionService.addCustomExtension(request.getExtension());
        return ResponseEntity.ok(ApiResponse.success("확장자가 추가되었습니다.", response));
    }

    /**
     * 커스텀 확장자 삭제
     */
    @DeleteMapping("/custom/{id}")
    @Operation(summary = "커스텀 확장자 삭제", description = "등록된 커스텀 확장자를 삭제합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "확장자를 찾을 수 없음")
    })
    public ResponseEntity<ApiResponse<Void>> deleteCustomExtension(
            @Parameter(description = "확장자 ID") @PathVariable Long id) {
        extensionService.deleteCustomExtension(id);
        return ResponseEntity.ok(ApiResponse.success("확장자가 삭제되었습니다.", null));
    }
}
