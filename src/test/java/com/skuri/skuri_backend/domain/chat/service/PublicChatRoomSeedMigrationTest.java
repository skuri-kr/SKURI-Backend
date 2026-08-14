package com.skuri.skuri_backend.domain.chat.service;

import com.skuri.skuri_backend.domain.chat.repository.ChatRoomRepository;
import com.skuri.skuri_backend.domain.member.service.DepartmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicChatRoomSeedMigrationTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private DepartmentService departmentService;

    @InjectMocks
    private PublicChatRoomSeedMigration seedMigration;

    @Test
    void seed_공개방이없으면_공식방과학과방을생성한다() {
        when(departmentService.getActiveDepartmentNames()).thenReturn(java.util.List.of("컴퓨터공학과", "경영학과"));
        when(chatRoomRepository.insertPublicSeedRoomIfAbsent(
                anyString(),
                anyString(),
                anyString(),
                nullable(String.class),
                nullable(String.class)
        ))
                .thenReturn(1);

        seedMigration.seed();

        verify(chatRoomRepository, times(4))
                .insertPublicSeedRoomIfAbsent(anyString(), anyString(), anyString(), nullable(String.class), nullable(String.class));
    }

    @Test
    void seed_이미존재하면_중복생성하지않는다() {
        when(departmentService.getActiveDepartmentNames()).thenReturn(java.util.List.of("컴퓨터공학과", "경영학과"));
        when(chatRoomRepository.insertPublicSeedRoomIfAbsent(
                anyString(),
                anyString(),
                anyString(),
                nullable(String.class),
                nullable(String.class)
        ))
                .thenReturn(0);

        seedMigration.seed();

        verify(chatRoomRepository, times(4))
                .insertPublicSeedRoomIfAbsent(anyString(), anyString(), anyString(), nullable(String.class), nullable(String.class));
    }
}
