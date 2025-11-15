package com.flow.blocker.service;

import com.flow.blocker.domain.CustomExtension;
import com.flow.blocker.domain.FixedExtension;
import com.flow.blocker.exception.ExtensionException;
import com.flow.blocker.repository.CustomExtensionRepository;
import com.flow.blocker.repository.FixedExtensionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileValidationService 테스트")
class FileValidationServiceTest {

    @InjectMocks
    private FileValidationService fileValidationService;

    @Mock
    private FixedExtensionRepository fixedExtensionRepository;

    @Mock
    private CustomExtensionRepository customExtensionRepository;

    @Nested
    @DisplayName("파일 검증")
    class ValidateFileTests {

        @Test
        @DisplayName("정상적인 파일은 검증을 통과한다")
        void validateFile_Success() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                "file", "document.txt", "text/plain", "Hello World".getBytes()
            );

			given(fixedExtensionRepository.findByCheckedTrue()).willReturn(Collections.emptyList());
			given(customExtensionRepository.findAll()).willReturn(Collections.emptyList());

            // when
            boolean result = fileValidationService.validateFile(file);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("빈 파일은 검증에 실패한다")
        void validateFile_EmptyFile() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                "file", "empty.txt", "text/plain", new byte[0]
            );

            // when & then
            assertThatThrownBy(() -> fileValidationService.validateFile(file))
                .isInstanceOf(ExtensionException.class)
                .hasMessage("파일이 비어있습니다.");
        }

        @Test
        @DisplayName("파일명이 없는 파일은 검증에 실패한다")
        void validateFile_NoFilename() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                "file", null, "text/plain", "content".getBytes()
            );

            // when & then
            assertThatThrownBy(() -> fileValidationService.validateFile(file))
                .isInstanceOf(ExtensionException.class)
                .hasMessage("파일명이 유효하지 않습니다.");
        }

        @Test
        @DisplayName("차단된 고정 확장자는 업로드할 수 없다")
        void validateFile_BlockedFixedExtension() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                "file", "malware.exe", "application/x-msdownload", "MZ".getBytes()
            );

			FixedExtension blockedExt = new FixedExtension("exe");
			blockedExt.updateChecked(true);

			given(fixedExtensionRepository.findByCheckedTrue())
					.willReturn(List.of(blockedExt));
			given(customExtensionRepository.findAll()).willReturn(Collections.emptyList());

            // when & then
            assertThatThrownBy(() -> fileValidationService.validateFile(file))
                .isInstanceOf(ExtensionException.class)
                .hasMessage("차단된 확장자입니다: .exe");
        }

        @Test
        @DisplayName("차단된 커스텀 확장자는 업로드할 수 없다")
        void validateFile_BlockedCustomExtension() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", "%PDF".getBytes()
            );

			CustomExtension blockedExt = new CustomExtension("pdf");

			given(fixedExtensionRepository.findByCheckedTrue()).willReturn(Collections.emptyList());
			given(customExtensionRepository.findAll())
					.willReturn(List.of(blockedExt));

            // when & then
            assertThatThrownBy(() -> fileValidationService.validateFile(file))
                .isInstanceOf(ExtensionException.class)
                .hasMessage("차단된 확장자입니다: .pdf");
        }

        @Test
        @DisplayName("이중 확장자는 업로드할 수 없다")
        void validateFile_DoubleExtension() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                "file", "document.jpg.exe", "image/jpeg", "fake image".getBytes()
            );

            // when & then
            assertThatThrownBy(() -> fileValidationService.validateFile(file))
                .isInstanceOf(ExtensionException.class)
                .hasMessage("이중 확장자는 허용되지 않습니다.");
        }

        @Test
        @DisplayName("연속된 점이 있는 파일명은 업로드할 수 없다")
        void validateFile_ConsecutiveDots() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                "file", "document..txt", "text/plain", "content".getBytes()
            );

            // when & then
            assertThatThrownBy(() -> fileValidationService.validateFile(file))
                .isInstanceOf(ExtensionException.class)
                .hasMessage("이중 확장자는 허용되지 않습니다.");
        }

        @Test
        @DisplayName("Null Byte Injection을 방지한다")
        void validateFile_NullByteInjection() {
            // given
            MockMultipartFile file = new MockMultipartFile(
                "file", "document.txt\0.exe", "text/plain", "content".getBytes()
            );

            // when & then
            assertThatThrownBy(() -> fileValidationService.validateFile(file))
                .isInstanceOf(ExtensionException.class)
                .hasMessage("유효하지 않은 파일명입니다.");
        }

        @Test
        @DisplayName("100MB를 초과하는 파일은 업로드할 수 없다")
        void validateFile_FileSizeExceeded() {
            // given
            byte[] largeContent = new byte[101 * 1024 * 1024]; // 101MB
            MockMultipartFile file = new MockMultipartFile(
                "file", "large.txt", "text/plain", largeContent
            );

            // when & then
            assertThatThrownBy(() -> fileValidationService.validateFile(file))
                .isInstanceOf(ExtensionException.class)
                .hasMessage("파일 크기는 100MB를 초과할 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("파일명 검증")
    class ValidateFilenameTests {

        @Test
        @DisplayName("정상적인 파일명은 검증을 통과한다")
        void validateFilename_Success() {
            // given
            String filename = "document.txt";
            
            given(fixedExtensionRepository.findByCheckedTrue()).willReturn(Collections.emptyList());
            given(customExtensionRepository.findAll()).willReturn(Collections.emptyList());

            // when
            boolean result = fileValidationService.validateFilename(filename);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("빈 파일명은 검증에 실패한다")
        void validateFilename_Empty() {
            // when & then
            assertThatThrownBy(() -> fileValidationService.validateFilename(""))
                .isInstanceOf(ExtensionException.class)
                .hasMessage("파일명이 유효하지 않습니다.");
        }

        @Test
        @DisplayName("차단된 확장자 파일명은 검증에 실패한다")
        void validateFilename_BlockedExtension() {
            // given
            FixedExtension blockedExt = new FixedExtension("exe");
            blockedExt.updateChecked(true);
            
            given(fixedExtensionRepository.findByCheckedTrue())
                .willReturn(List.of(blockedExt));
            given(customExtensionRepository.findAll()).willReturn(Collections.emptyList());

            // when & then
            assertThatThrownBy(() -> fileValidationService.validateFilename("virus.exe"))
                .isInstanceOf(ExtensionException.class)
                .hasMessage("차단된 확장자입니다: .exe");
        }

        @Test
        @DisplayName("이중 확장자 파일명은 검증에 실패한다")
        void validateFilename_DoubleExtension() {
            // when & then
            assertThatThrownBy(() -> fileValidationService.validateFilename("image.jpg.exe"))
                .isInstanceOf(ExtensionException.class)
                .hasMessage("이중 확장자는 허용되지 않습니다.");
        }

        @Test
        @DisplayName("XSS 공격 문자가 제거된다")
        void validateFilename_XSSPrevention() {
            // given
            String maliciousFilename = "<script>alert('xss')</script>.txt";
            
            given(fixedExtensionRepository.findByCheckedTrue()).willReturn(Collections.emptyList());
            given(customExtensionRepository.findAll()).willReturn(Collections.emptyList());

            // when
            boolean result = fileValidationService.validateFilename(maliciousFilename);

            // then
            assertThat(result).isTrue();
            // XSS 문자가 제거되어 정상 처리됨
        }
    }

    @Nested
    @DisplayName("차단된 확장자 목록 조회")
    class GetBlockedExtensionsTests {

        @Test
        @DisplayName("차단된 고정 확장자와 커스텀 확장자를 모두 반환한다")
        void getBlockedExtensions_All() {
            // given
            FixedExtension fixedExt = new FixedExtension("exe");
            fixedExt.updateChecked(true);
            CustomExtension customExt = new CustomExtension("pdf");
            
            given(fixedExtensionRepository.findByCheckedTrue())
                .willReturn(List.of(fixedExt));
            given(customExtensionRepository.findAll())
                .willReturn(List.of(customExt));

            // when
            Set<String> blocked = fileValidationService.getBlockedExtensions();

            // then
            assertThat(blocked).containsExactlyInAnyOrder("exe", "pdf");
        }

        @Test
        @DisplayName("체크되지 않은 고정 확장자는 포함하지 않는다")
        void getBlockedExtensions_UncheckedFixed() {
            // given
            FixedExtension checkedExt = new FixedExtension("exe");
            checkedExt.updateChecked(true);
            
            FixedExtension uncheckedExt = new FixedExtension("bat");
            uncheckedExt.updateChecked(false);
            
            given(fixedExtensionRepository.findByCheckedTrue())
                .willReturn(List.of(checkedExt));
            given(customExtensionRepository.findAll())
                .willReturn(Collections.emptyList());

            // when
            Set<String> blocked = fileValidationService.getBlockedExtensions();

            // then
            assertThat(blocked).containsExactly("exe");
            assertThat(blocked).doesNotContain("bat");
        }
    }
}
