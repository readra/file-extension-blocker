package com.flow.blocker.service;

import com.flow.blocker.domain.CustomExtension;
import com.flow.blocker.domain.FixedExtension;
import com.flow.blocker.dto.ExtensionResponse;
import com.flow.blocker.event.CacheEvictionEvent;
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
import org.springframework.context.ApplicationEventPublisher;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExtensionService 테스트")
class ExtensionServiceTest {

    @InjectMocks
    private ExtensionService extensionService;

    @Mock
    private FixedExtensionRepository fixedExtensionRepository;

    @Mock
    private CustomExtensionRepository customExtensionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Nested
    @DisplayName("고정 확장자 관리")
    class FixedExtensionTests {

        @Test
        @DisplayName("고정 확장자 목록을 조회할 수 있다")
        void getAllFixedExtensions() {
            // given
            List<FixedExtension> extensions = Arrays.asList(
                new FixedExtension("exe"),
                new FixedExtension("bat")
            );
            given(fixedExtensionRepository.findAll()).willReturn(extensions);

            // when
            List<ExtensionResponse> result = extensionService.getAllFixedExtensions();

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).extension()).isEqualTo("exe");
            assertThat(result.get(1).extension()).isEqualTo("bat");
        }

        @Test
        @DisplayName("고정 확장자의 체크 상태를 변경할 수 있다")
        void updateFixedExtensionCheck() {
            // given
            FixedExtension extension = new FixedExtension("exe");
            given(fixedExtensionRepository.findByExtension("exe"))
                .willReturn(Optional.of(extension));

            // when
            ExtensionResponse result = extensionService.updateFixedExtensionCheck("exe", true);

            // then
            assertThat(result.checked()).isTrue();
            verify(eventPublisher).publishEvent(any(CacheEvictionEvent.ExtensionChangeEvent.class));
        }

        @Test
        @DisplayName("존재하지 않는 고정 확장자 상태 변경시 예외가 발생한다")
        void updateFixedExtensionCheck_NotFound() {
            // given
            given(fixedExtensionRepository.findByExtension("unknown"))
                .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> 
                extensionService.updateFixedExtensionCheck("unknown", true))
                .isInstanceOf(ExtensionException.class)
                .hasMessage("존재하지 않는 고정 확장자입니다.");
        }

        @Test
        @DisplayName("초기 고정 확장자를 세팅할 수 있다")
        void initializeFixedExtensions() {
            // given
            given(fixedExtensionRepository.existsByExtension(anyString())).willReturn(false);

            // when
            extensionService.initializeFixedExtensions();

            // then
            verify(fixedExtensionRepository, times(7)).save(any(FixedExtension.class));
        }
    }

    @Nested
    @DisplayName("커스텀 확장자 추가")
    class AddCustomExtensionTests {

        @Test
        @DisplayName("정상적인 커스텀 확장자를 추가할 수 있다")
        void addCustomExtension_Success() {
            // given
            String extension = "pdf";
            CustomExtension saved = new CustomExtension(extension);
            saved.setId(1L);
            
            given(customExtensionRepository.countBy()).willReturn(0L);
            given(fixedExtensionRepository.existsByExtension(extension)).willReturn(false);
            given(customExtensionRepository.existsByExtension(extension)).willReturn(false);
            given(customExtensionRepository.save(any(CustomExtension.class))).willReturn(saved);

            // when
            ExtensionResponse result = extensionService.addCustomExtension(extension);

            // then
            assertThat(result.extension()).isEqualTo(extension);
            assertThat(result.id()).isEqualTo(1L);
            verify(eventPublisher).publishEvent(any(CacheEvictionEvent.ExtensionChangeEvent.class));
        }

        @Test
        @DisplayName("빈 문자열은 추가할 수 없다")
        void addCustomExtension_EmptyString() {
            // when & then
            assertThatThrownBy(() -> extensionService.addCustomExtension(""))
                .isInstanceOf(ExtensionException.class)
                .hasMessage("확장자를 입력해주세요.");

            assertThatThrownBy(() -> extensionService.addCustomExtension("  "))
                .isInstanceOf(ExtensionException.class)
                .hasMessage("확장자를 입력해주세요.");

            assertThatThrownBy(() -> extensionService.addCustomExtension(null))
                .isInstanceOf(ExtensionException.class)
                .hasMessage("확장자를 입력해주세요.");
        }

        @Test
        @DisplayName("20자를 초과하는 확장자는 추가할 수 없다")
        void addCustomExtension_TooLong() {
            // given
            String longExtension = "a".repeat(21);

            // when & then
            assertThatThrownBy(() -> extensionService.addCustomExtension(longExtension))
                .isInstanceOf(ExtensionException.class)
                .hasMessage("확장자는 최대 20자까지 입력 가능합니다.");
        }

        @Test
        @DisplayName("영문자와 숫자 이외의 문자는 허용하지 않는다")
        void addCustomExtension_InvalidCharacters() {
            // when & then
            assertThatThrownBy(() -> extensionService.addCustomExtension("test-ext"))
                .isInstanceOf(ExtensionException.class)
                .hasMessage("확장자는 영문자와 숫자만 입력 가능합니다.");

            assertThatThrownBy(() -> extensionService.addCustomExtension("test@ext"))
                .isInstanceOf(ExtensionException.class)
                .hasMessage("확장자는 영문자와 숫자만 입력 가능합니다.");

            assertThatThrownBy(() -> extensionService.addCustomExtension("한글"))
                .isInstanceOf(ExtensionException.class)
                .hasMessage("확장자는 영문자와 숫자만 입력 가능합니다.");
        }

        @Test
        @DisplayName("점(.)으로 시작하는 확장자도 정상 처리한다")
        void addCustomExtension_WithDot() {
            // given
            String extension = ".pdf";
            CustomExtension saved = new CustomExtension("pdf");
            saved.setId(1L);
            
            given(customExtensionRepository.countBy()).willReturn(0L);
            given(fixedExtensionRepository.existsByExtension("pdf")).willReturn(false);
            given(customExtensionRepository.existsByExtension("pdf")).willReturn(false);
            given(customExtensionRepository.save(any(CustomExtension.class))).willReturn(saved);

            // when
            ExtensionResponse result = extensionService.addCustomExtension(extension);

            // then
            assertThat(result.extension()).isEqualTo("pdf");
        }

        @Test
        @DisplayName("대문자로 입력된 확장자는 소문자로 변환된다")
        void addCustomExtension_UpperCase() {
            // given
            String extension = "PDF";
            CustomExtension saved = new CustomExtension("pdf");
            saved.setId(1L);
            
            given(customExtensionRepository.countBy()).willReturn(0L);
            given(fixedExtensionRepository.existsByExtension("pdf")).willReturn(false);
            given(customExtensionRepository.existsByExtension("pdf")).willReturn(false);
            given(customExtensionRepository.save(any(CustomExtension.class))).willReturn(saved);

            // when
            ExtensionResponse result = extensionService.addCustomExtension(extension);

            // then
            assertThat(result.extension()).isEqualTo("pdf");
        }

        @Test
        @DisplayName("200개 제한을 초과하면 추가할 수 없다")
        void addCustomExtension_ExceedLimit() {
            // given
            given(customExtensionRepository.countBy()).willReturn(200L);

            // when & then
            assertThatThrownBy(() -> extensionService.addCustomExtension("pdf"))
                .isInstanceOf(ExtensionException.class)
                .hasMessage("커스텀 확장자는 최대 200개까지만 추가 가능합니다.");
        }

        @Test
        @DisplayName("고정 확장자와 중복되면 추가할 수 없다")
        void addCustomExtension_DuplicateWithFixed() {
            // given
            given(customExtensionRepository.countBy()).willReturn(0L);
            given(fixedExtensionRepository.existsByExtension("exe")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> extensionService.addCustomExtension("exe"))
                .isInstanceOf(ExtensionException.class)
                .hasMessage("이미 고정 확장자에 존재합니다.");
        }

        @Test
        @DisplayName("이미 추가된 커스텀 확장자는 중복 추가할 수 없다")
        void addCustomExtension_DuplicateWithCustom() {
            // given
            given(customExtensionRepository.countBy()).willReturn(0L);
            given(fixedExtensionRepository.existsByExtension("pdf")).willReturn(false);
            given(customExtensionRepository.existsByExtension("pdf")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> extensionService.addCustomExtension("pdf"))
                .isInstanceOf(ExtensionException.class)
                .hasMessage("이미 추가된 확장자입니다.");
        }
    }

    @Nested
    @DisplayName("커스텀 확장자 삭제")
    class DeleteCustomExtensionTests {

        @Test
        @DisplayName("커스텀 확장자를 삭제할 수 있다")
        void deleteCustomExtension_Success() {
            // given
            CustomExtension extension = new CustomExtension("pdf");
            extension.setId(1L);
            given(customExtensionRepository.findById(1L))
                .willReturn(Optional.of(extension));

            // when
            extensionService.deleteCustomExtension(1L);

            // then
            verify(customExtensionRepository).delete(extension);
            verify(eventPublisher).publishEvent(any(CacheEvictionEvent.ExtensionChangeEvent.class));
        }

        @Test
        @DisplayName("존재하지 않는 확장자는 삭제할 수 없다")
        void deleteCustomExtension_NotFound() {
            // given
            given(customExtensionRepository.findById(999L))
                .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> extensionService.deleteCustomExtension(999L))
                .isInstanceOf(ExtensionException.class)
                .hasMessage("존재하지 않는 확장자입니다.");
        }
    }

    @Nested
    @DisplayName("커스텀 확장자 조회")
    class GetCustomExtensionsTests {

        @Test
        @DisplayName("커스텀 확장자 목록을 조회할 수 있다")
        void getAllCustomExtensions() {
            // given
            CustomExtension ext1 = new CustomExtension("pdf");
            ext1.setId(1L);
            CustomExtension ext2 = new CustomExtension("doc");
            ext2.setId(2L);
            
            List<CustomExtension> extensions = Arrays.asList(ext1, ext2);
            given(customExtensionRepository.findAll()).willReturn(extensions);

            // when
            List<ExtensionResponse> result = extensionService.getAllCustomExtensions();

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).extension()).isEqualTo("pdf");
            assertThat(result.get(1).extension()).isEqualTo("doc");
        }
    }
}
