package com.flow.blocker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flow.blocker.dto.ExtensionRequest;
import com.flow.blocker.dto.ExtensionResponse;
import com.flow.blocker.exception.ExtensionException;
import com.flow.blocker.service.ExtensionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExtensionApiController.class)
@DisplayName("ExtensionApiController 테스트")
class ExtensionApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExtensionService extensionService;

    @Nested
    @DisplayName("GET /api/extensions/fixed")
    class GetFixedExtensions {

        @Test
        @DisplayName("고정 확장자 목록을 조회할 수 있다")
        void getFixedExtensions_Success() throws Exception {
            // given
            ExtensionResponse ext1 = ExtensionResponse.builder()
                .id(1L)
                .extension("exe")
                .checked(true)
                .build();
            ExtensionResponse ext2 = ExtensionResponse.builder()
                .id(2L)
                .extension("bat")
                .checked(false)
                .build();
            List<ExtensionResponse> extensions = Arrays.asList(ext1, ext2);
            
            given(extensionService.getAllFixedExtensions()).willReturn(extensions);

            // when & then
            mockMvc.perform(get("/api/extensions/fixed"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].extension").value("exe"))
                .andExpect(jsonPath("$.data[0].checked").value(true))
                .andExpect(jsonPath("$.data[1].extension").value("bat"))
                .andExpect(jsonPath("$.data[1].checked").value(false));
        }

        @Test
        @DisplayName("고정 확장자가 없는 경우 빈 배열을 반환한다")
        void getFixedExtensions_Empty() throws Exception {
            // given
            given(extensionService.getAllFixedExtensions()).willReturn(Collections.emptyList());

            // when & then
            mockMvc.perform(get("/api/extensions/fixed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
        }
    }

    @Nested
    @DisplayName("PATCH /api/extensions/fixed/{extension}")
    class UpdateFixedExtension {

        @Test
        @DisplayName("고정 확장자의 체크 상태를 변경할 수 있다")
        void updateFixedExtension_Success() throws Exception {
            // given
            ExtensionResponse response = ExtensionResponse.builder()
                .id(1L)
                .extension("exe")
                .checked(true)
                .build();
            
            given(extensionService.updateFixedExtensionCheck("exe", true))
                .willReturn(response);

            Map<String, Boolean> request = Map.of("checked", true);

            // when & then
            mockMvc.perform(patch("/api/extensions/fixed/exe")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.extension").value("exe"))
                .andExpect(jsonPath("$.data.checked").value(true));
        }

        @Test
        @DisplayName("존재하지 않는 고정 확장자 변경 시 에러를 반환한다")
        void updateFixedExtension_NotFound() throws Exception {
            // given
            given(extensionService.updateFixedExtensionCheck("unknown", true))
                .willThrow(new ExtensionException("존재하지 않는 고정 확장자입니다."));

            Map<String, Boolean> request = Map.of("checked", true);

            // when & then
            mockMvc.perform(patch("/api/extensions/fixed/unknown")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("존재하지 않는 고정 확장자입니다."));
        }
    }

    @Nested
    @DisplayName("GET /api/extensions/custom")
    class GetCustomExtensions {

        @Test
        @DisplayName("커스텀 확장자 목록을 조회할 수 있다")
        void getCustomExtensions_Success() throws Exception {
            // given
            ExtensionResponse ext1 = ExtensionResponse.builder()
                .id(1L)
                .extension("pdf")
                .checked(true)
                .build();
            ExtensionResponse ext2 = ExtensionResponse.builder()
                .id(2L)
                .extension("doc")
                .checked(true)
                .build();
            List<ExtensionResponse> extensions = Arrays.asList(ext1, ext2);
            
            given(extensionService.getAllCustomExtensions()).willReturn(extensions);

            // when & then
            mockMvc.perform(get("/api/extensions/custom"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].extension").value("pdf"))
                .andExpect(jsonPath("$.data[1].extension").value("doc"));
        }
    }

    @Nested
    @DisplayName("POST /api/extensions/custom")
    class AddCustomExtension {

        @Test
        @DisplayName("커스텀 확장자를 추가할 수 있다")
        void addCustomExtension_Success() throws Exception {
            // given
            ExtensionRequest request = new ExtensionRequest();
            request.setExtension("pdf");

            ExtensionResponse response = ExtensionResponse.builder()
                .id(1L)
                .extension("pdf")
                .checked(true)
                .build();
            
            given(extensionService.addCustomExtension("pdf")).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/extensions/custom")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("확장자가 추가되었습니다."))
                .andExpect(jsonPath("$.data.extension").value("pdf"));
        }

        @Test
        @DisplayName("빈 확장자는 추가할 수 없다")
        void addCustomExtension_EmptyExtension() throws Exception {
            // given
            ExtensionRequest request = new ExtensionRequest();
            request.setExtension("");

            // when & then
            mockMvc.perform(post("/api/extensions/custom")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("null 확장자는 추가할 수 없다")
        void addCustomExtension_NullExtension() throws Exception {
            // given
            ExtensionRequest request = new ExtensionRequest();

            // when & then
            mockMvc.perform(post("/api/extensions/custom")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("21자 이상의 확장자는 추가할 수 없다")
        void addCustomExtension_TooLongExtension() throws Exception {
            // given
            ExtensionRequest request = new ExtensionRequest();
            request.setExtension("a".repeat(21));

            // when & then
            mockMvc.perform(post("/api/extensions/custom")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("중복된 확장자 추가 시 에러를 반환한다")
        void addCustomExtension_Duplicate() throws Exception {
            // given
            ExtensionRequest request = new ExtensionRequest();
            request.setExtension("pdf");
            
            given(extensionService.addCustomExtension("pdf"))
                .willThrow(new ExtensionException("이미 추가된 확장자입니다."));

            // when & then
            mockMvc.perform(post("/api/extensions/custom")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이미 추가된 확장자입니다."));
        }

        @Test
        @DisplayName("200개 제한 초과 시 에러를 반환한다")
        void addCustomExtension_ExceedLimit() throws Exception {
            // given
            ExtensionRequest request = new ExtensionRequest();
            request.setExtension("pdf");
            
            given(extensionService.addCustomExtension("pdf"))
                .willThrow(new ExtensionException("커스텀 확장자는 최대 200개까지만 추가 가능합니다."));

            // when & then
            mockMvc.perform(post("/api/extensions/custom")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("커스텀 확장자는 최대 200개까지만 추가 가능합니다."));
        }
    }

    @Nested
    @DisplayName("DELETE /api/extensions/custom/{id}")
    class DeleteCustomExtension {

        @Test
        @DisplayName("커스텀 확장자를 삭제할 수 있다")
        void deleteCustomExtension_Success() throws Exception {
            // given
            willDoNothing().given(extensionService).deleteCustomExtension(1L);

            // when & then
            mockMvc.perform(delete("/api/extensions/custom/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("확장자가 삭제되었습니다."));
        }

        @Test
        @DisplayName("존재하지 않는 확장자 삭제 시 에러를 반환한다")
        void deleteCustomExtension_NotFound() throws Exception {
            // given
            willThrow(new ExtensionException("존재하지 않는 확장자입니다."))
                .given(extensionService).deleteCustomExtension(999L);

            // when & then
            mockMvc.perform(delete("/api/extensions/custom/999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("존재하지 않는 확장자입니다."));
        }
    }
}
