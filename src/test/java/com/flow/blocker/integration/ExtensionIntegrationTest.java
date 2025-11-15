package com.flow.blocker.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flow.blocker.dto.ExtensionRequest;
import com.flow.blocker.repository.CustomExtensionRepository;
import com.flow.blocker.repository.FixedExtensionRepository;
import com.flow.blocker.service.ExtensionService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
//@TestPropertySource(properties = {
//    "spring.datasource.url=jdbc:h2:mem:testdb",
//    "spring.jpa.hibernate.ddl-auto=create-drop"
//})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("확장자 관리 통합 테스트")
class ExtensionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExtensionService extensionService;

    @Autowired
    private FixedExtensionRepository fixedExtensionRepository;

    @Autowired
    private CustomExtensionRepository customExtensionRepository;

    @BeforeEach
    void setUp() {
        // 고정 확장자 초기화
        extensionService.initializeFixedExtensions();
    }

    @AfterEach
    void tearDown() {
        // 테스트 후 데이터 정리
        customExtensionRepository.deleteAll();
    }

    @Nested
    @DisplayName("고정 확장자 관리")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class FixedExtensionTests {

        @Test
        @Order(1)
        @DisplayName("고정 확장자 목록을 조회한다")
        void getFixedExtensions() throws Exception {
            mockMvc.perform(get("/api/extensions/fixed"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(7))) // 초기 7개 고정 확장자
                .andExpect(jsonPath("$.data[*].extension", 
                    containsInAnyOrder("bat", "cmd", "com", "cpl", "exe", "scr", "js")))
                .andExpect(jsonPath("$.data[*].checked", 
                    everyItem(equalTo(false)))); // 초기값은 모두 unchecked
        }

        @Test
        @Order(2)
        @DisplayName("고정 확장자를 활성화한다")
        void activateFixedExtension() throws Exception {
            Map<String, Boolean> request = Map.of("checked", true);

            mockMvc.perform(patch("/api/extensions/fixed/exe")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.extension").value("exe"))
                .andExpect(jsonPath("$.data.checked").value(true));

            // 상태 확인
            mockMvc.perform(get("/api/extensions/fixed"))
                .andExpect(jsonPath("$.data[?(@.extension == 'exe')].checked", 
                    contains(true)));
        }

        @Test
        @Order(3)
        @DisplayName("고정 확장자를 비활성화한다")
        void deactivateFixedExtension() throws Exception {
            // 먼저 활성화
            Map<String, Boolean> activateRequest = Map.of("checked", true);
            mockMvc.perform(patch("/api/extensions/fixed/bat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(activateRequest)));

            // 비활성화
            Map<String, Boolean> deactivateRequest = Map.of("checked", false);
            mockMvc.perform(patch("/api/extensions/fixed/bat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(deactivateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checked").value(false));
        }

        @Test
        @Order(4)
        @DisplayName("존재하지 않는 고정 확장자 변경 시 에러가 발생한다")
        void updateNonExistentFixedExtension() throws Exception {
            Map<String, Boolean> request = Map.of("checked", true);

            mockMvc.perform(patch("/api/extensions/fixed/unknown")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("존재하지 않는 고정 확장자입니다."));
        }
    }

    @Nested
    @DisplayName("커스텀 확장자 관리")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class CustomExtensionTests {

        @Test
        @Order(1)
        @DisplayName("커스텀 확장자를 추가한다")
        void addCustomExtension() throws Exception {
            ExtensionRequest request = new ExtensionRequest();
            request.setExtension("pdf");

            mockMvc.perform(post("/api/extensions/custom")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("확장자가 추가되었습니다."))
                .andExpect(jsonPath("$.data.extension").value("pdf"));

            // 추가 확인
            mockMvc.perform(get("/api/extensions/custom"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].extension").value("pdf"));
        }

        @Test
        @Order(2)
        @DisplayName("대문자 확장자는 소문자로 변환되어 저장된다")
        void addCustomExtensionUpperCase() throws Exception {
            ExtensionRequest request = new ExtensionRequest();
            request.setExtension("PDF");

            mockMvc.perform(post("/api/extensions/custom")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.extension").value("pdf"));
        }

        @Test
        @Order(3)
        @DisplayName("점(.)으로 시작하는 확장자도 정상 처리된다")
        void addCustomExtensionWithDot() throws Exception {
            ExtensionRequest request = new ExtensionRequest();
            request.setExtension(".doc");

            mockMvc.perform(post("/api/extensions/custom")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.extension").value("doc"));
        }

        @Test
        @Order(4)
        @DisplayName("중복된 커스텀 확장자는 추가할 수 없다")
        void addDuplicateCustomExtension() throws Exception {
            // 첫 번째 추가
            ExtensionRequest request1 = new ExtensionRequest();
            request1.setExtension("xlsx");
            mockMvc.perform(post("/api/extensions/custom")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

            // 중복 추가 시도
            ExtensionRequest request2 = new ExtensionRequest();
            request2.setExtension("xlsx");
            mockMvc.perform(post("/api/extensions/custom")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미 추가된 확장자입니다."));
        }

        @Test
        @Order(5)
        @DisplayName("고정 확장자와 중복되는 커스텀 확장자는 추가할 수 없다")
        void addCustomExtensionDuplicateWithFixed() throws Exception {
            ExtensionRequest request = new ExtensionRequest();
            request.setExtension("exe");

            mockMvc.perform(post("/api/extensions/custom")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미 고정 확장자에 존재합니다."));
        }

        @Test
        @Order(6)
        @DisplayName("커스텀 확장자를 삭제한다")
        @Transactional
        void deleteCustomExtension() throws Exception {
            // 추가
            ExtensionRequest request = new ExtensionRequest();
            request.setExtension("temp");
            
            String response = mockMvc.perform(post("/api/extensions/custom")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

            long id = objectMapper.readTree(response)
                .path("data")
                .path("id")
                .asLong();

            // 삭제
            mockMvc.perform(delete("/api/extensions/custom/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("확장자가 삭제되었습니다."));

            // 삭제 확인
            mockMvc.perform(get("/api/extensions/custom"))
                .andExpect(jsonPath("$.data[?(@.extension == 'temp')]").doesNotExist());
        }

        @Test
        @Order(7)
        @DisplayName("빈 문자열 확장자는 추가할 수 없다")
        void addEmptyExtension() throws Exception {
            ExtensionRequest request = new ExtensionRequest();
            request.setExtension("");

            mockMvc.perform(post("/api/extensions/custom")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @Order(8)
        @DisplayName("20자를 초과하는 확장자는 추가할 수 없다")
        void addTooLongExtension() throws Exception {
            ExtensionRequest request = new ExtensionRequest();
            request.setExtension("a".repeat(21));

            mockMvc.perform(post("/api/extensions/custom")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @Order(9)
        @DisplayName("특수문자가 포함된 확장자는 추가할 수 없다")
        void addExtensionWithSpecialCharacters() throws Exception {
            ExtensionRequest request = new ExtensionRequest();
            request.setExtension("test@ext");

            mockMvc.perform(post("/api/extensions/custom")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("확장자는 영문자와 숫자만 입력 가능합니다."));
        }
    }

    @Nested
    @DisplayName("대용량 테스트")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class BulkOperationTests {

        @Test
        @DisplayName("200개 제한 테스트 - 199개까지는 추가 가능")
        @Transactional
        void addUpTo199CustomExtensions() throws Exception {
            // 199개 추가
            for (int i = 1; i <= 199; i++) {
                ExtensionRequest request = new ExtensionRequest();
                request.setExtension("ext" + i);
                
                mockMvc.perform(post("/api/extensions/custom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
            }

            // 200번째 추가
            ExtensionRequest request200 = new ExtensionRequest();
            request200.setExtension("ext200");
            
            mockMvc.perform(post("/api/extensions/custom")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request200)))
                .andExpect(status().isOk());

            // 201번째 추가 시도 - 실패해야 함
            ExtensionRequest request201 = new ExtensionRequest();
            request201.setExtension("ext201");
            
            mockMvc.perform(post("/api/extensions/custom")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request201)))
                .andExpect(status().is(oneOf(400, 429)))
                .andExpect(jsonPath("$.message")
                    .value("커스텀 확장자는 최대 200개까지만 추가 가능합니다."));
        }
    }
}
