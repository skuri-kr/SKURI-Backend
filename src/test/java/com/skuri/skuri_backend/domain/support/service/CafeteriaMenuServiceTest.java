package com.skuri.skuri_backend.domain.support.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.support.dto.request.CafeteriaMenuBadgeRequest;
import com.skuri.skuri_backend.domain.support.dto.request.CafeteriaMenuEntryRequest;
import com.skuri.skuri_backend.domain.support.dto.request.CreateCafeteriaMenuRequest;
import com.skuri.skuri_backend.domain.support.dto.request.UpdateCafeteriaMenuRequest;
import com.skuri.skuri_backend.domain.support.dto.response.CafeteriaMenuEntryResponse;
import com.skuri.skuri_backend.domain.support.dto.response.CafeteriaMenuResponse;
import com.skuri.skuri_backend.domain.support.entity.CafeteriaMenu;
import com.skuri.skuri_backend.domain.support.model.CafeteriaMenuBadgeMetadata;
import com.skuri.skuri_backend.domain.support.model.CafeteriaMenuEntryMetadata;
import com.skuri.skuri_backend.domain.support.model.CafeteriaMenuIdCodec;
import com.skuri.skuri_backend.domain.support.model.CafeteriaMenuReactionType;
import com.skuri.skuri_backend.domain.support.repository.CafeteriaMenuReactionRepository;
import com.skuri.skuri_backend.domain.support.repository.CafeteriaMenuRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CafeteriaMenuServiceTest {

    @Mock
    private CafeteriaMenuRepository cafeteriaMenuRepository;

    @Mock
    private CafeteriaMenuReactionRepository cafeteriaMenuReactionRepository;

    @InjectMocks
    private CafeteriaMenuService cafeteriaMenuService;

    @Test
    void 공개미리보기조회는_반응집계쿼리를실행하지않는다() {
        when(cafeteriaMenuRepository.findById("2026-W08")).thenReturn(Optional.of(CafeteriaMenu.create(
                "2026-W08",
                LocalDate.of(2026, 2, 16),
                LocalDate.of(2026, 2, 20),
                Map.of("2026-02-16", Map.of("rollNoodles", List.of("우동"))),
                Map.of()
        )));

        CafeteriaMenuResponse response = cafeteriaMenuService.getCurrentWeekMenuWithoutReactions(
                LocalDate.of(2026, 2, 16)
        );

        assertEquals("우동", response.menuEntries().get("2026-02-16").get("rollNoodles").getFirst().title());
        verifyNoInteractions(cafeteriaMenuReactionRepository);
    }

    @Test
    void createMenu_menuEntries만전달하면_menus를역생성하고관리자count는무시한다() {
        when(cafeteriaMenuRepository.existsById("2026-W08")).thenReturn(false);
        when(cafeteriaMenuRepository.save(any(CafeteriaMenu.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CafeteriaMenuResponse response = cafeteriaMenuService.createMenu(new CreateCafeteriaMenuRequest(
                "2026-W08",
                LocalDate.of(2026, 2, 16),
                LocalDate.of(2026, 2, 20),
                null,
                Map.of(
                        "2026-02-16",
                        Map.of(
                                "rollNoodles",
                                List.of(new CafeteriaMenuEntryRequest(
                                        "존슨부대찌개",
                                        List.of(new CafeteriaMenuBadgeRequest(null, "테이크아웃")),
                                        178,
                                        22
                                ))
                        )
                )
        ));

        ArgumentCaptor<CafeteriaMenu> captor = ArgumentCaptor.forClass(CafeteriaMenu.class);
        verify(cafeteriaMenuRepository).save(captor.capture());

        assertIterableEquals(
                List.of("존슨부대찌개"),
                captor.getValue().getMenus().get("2026-02-16").get("rollNoodles")
        );
        assertEquals(0, captor.getValue().getMenuEntries().get("2026-02-16").get("rollNoodles").get(0).likeCount());
        assertEquals("테이크아웃", captor.getValue().getMenuEntries().get("2026-02-16").get("rollNoodles").get(0).badges().get(0).label());
        assertEquals("Roll & Noodles", response.categories().get(0).label());
        assertEquals(0, response.menuEntries().get("2026-02-16").get("rollNoodles").get(0).dislikeCount());
    }

    @Test
    void createMenu_기존menus만전달하면_기본메타데이터를생성한다() {
        when(cafeteriaMenuRepository.existsById("2026-W08")).thenReturn(false);
        when(cafeteriaMenuRepository.save(any(CafeteriaMenu.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CafeteriaMenuResponse response = cafeteriaMenuService.createMenu(new CreateCafeteriaMenuRequest(
                "2026-W08",
                LocalDate.of(2026, 2, 16),
                LocalDate.of(2026, 2, 20),
                Map.of(
                        "2026-02-16",
                        Map.of("rollNoodles", List.of("우동", "김밥"))
                ),
                null
        ));

        assertEquals(0, response.menuEntries().get("2026-02-16").get("rollNoodles").get(0).likeCount());
        assertTrue(response.menuEntries().get("2026-02-16").get("rollNoodles").get(0).badges().isEmpty());
        assertTrue(response.menuEntries().get("2026-02-16").get("theBab").isEmpty());
    }

    @Test
    void createMenu_menus가빈카테고리를생략해도_menuEntries의빈배열과동일하게본다() {
        when(cafeteriaMenuRepository.existsById("2026-W08")).thenReturn(false);
        when(cafeteriaMenuRepository.save(any(CafeteriaMenu.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CafeteriaMenuResponse response = cafeteriaMenuService.createMenu(new CreateCafeteriaMenuRequest(
                "2026-W08",
                LocalDate.of(2026, 2, 16),
                LocalDate.of(2026, 2, 20),
                Map.of(
                        "2026-02-16",
                        Map.of("rollNoodles", List.of("존슨부대찌개"))
                ),
                Map.of(
                        "2026-02-16",
                        Map.of(
                                "rollNoodles",
                                List.of(new CafeteriaMenuEntryRequest(
                                        "존슨부대찌개",
                                        List.of(new CafeteriaMenuBadgeRequest("TAKEOUT", "테이크아웃")),
                                        178,
                                        22
                                )),
                                "theBab",
                                List.of(),
                                "fryRice",
                                List.of()
                        )
                )
        ));

        assertIterableEquals(
                List.of("존슨부대찌개"),
                response.menus().get("2026-02-16").get("rollNoodles")
        );
        assertTrue(response.menuEntries().get("2026-02-16").get("theBab").isEmpty());
        assertTrue(response.menuEntries().get("2026-02-16").get("fryRice").isEmpty());
    }

    @Test
    void updateMenu_응답을그대로라운드트립해도_빈카테고리차이로실패하지않는다() {
        when(cafeteriaMenuRepository.findById("2026-W08")).thenReturn(Optional.of(CafeteriaMenu.create(
                "2026-W08",
                LocalDate.of(2026, 2, 16),
                LocalDate.of(2026, 2, 20),
                Map.of(
                        "2026-02-16",
                        Map.of("rollNoodles", List.of("존슨부대찌개"))
                ),
                Map.of(
                        "2026-02-16",
                        Map.of(
                                "rollNoodles",
                                List.of(new CafeteriaMenuEntryMetadata(
                                        "존슨부대찌개",
                                        List.of(new CafeteriaMenuBadgeMetadata("TAKEOUT", "테이크아웃")),
                                        0,
                                        0
                                ))
                        )
                )
        )));

        CafeteriaMenuResponse response = cafeteriaMenuService.updateMenu(
                "2026-W08",
                new UpdateCafeteriaMenuRequest(
                        LocalDate.of(2026, 2, 16),
                        LocalDate.of(2026, 2, 20),
                        Map.of(
                                "2026-02-16",
                                Map.of("rollNoodles", List.of("존슨부대찌개"))
                        ),
                        Map.of(
                                "2026-02-16",
                                Map.of(
                                        "rollNoodles",
                                        List.of(new CafeteriaMenuEntryRequest(
                                                "존슨부대찌개",
                                                List.of(new CafeteriaMenuBadgeRequest("TAKEOUT", "테이크아웃")),
                                                178,
                                                22
                                        )),
                                        "theBab",
                                        List.of(),
                                        "fryRice",
                                        List.of()
                                )
                        )
                )
        );

        assertIterableEquals(
                List.of("존슨부대찌개"),
                response.menus().get("2026-02-16").get("rollNoodles")
        );
        assertTrue(response.menuEntries().get("2026-02-16").get("theBab").isEmpty());
        assertTrue(response.menuEntries().get("2026-02-16").get("fryRice").isEmpty());
    }

    @Test
    void updateMenu_삭제된메뉴의반응은정리한다() {
        when(cafeteriaMenuRepository.findById("2026-W08")).thenReturn(Optional.of(CafeteriaMenu.create(
                "2026-W08",
                LocalDate.of(2026, 2, 16),
                LocalDate.of(2026, 2, 20),
                Map.of("2026-02-16", Map.of("rollNoodles", List.of("우동", "김밥"))),
                Map.of()
        )));

        cafeteriaMenuService.updateMenu(
                "2026-W08",
                new UpdateCafeteriaMenuRequest(
                        LocalDate.of(2026, 2, 16),
                        LocalDate.of(2026, 2, 20),
                        Map.of("2026-02-16", Map.of("rollNoodles", List.of("우동"))),
                        null
                )
        );

        verify(cafeteriaMenuReactionRepository).deleteObsoleteReactions(
                eq("2026-W08"),
                eq(Set.of(CafeteriaMenuIdCodec.encode("2026-W08", "rollNoodles", "우동")))
        );
    }

    @Test
    void deleteMenu_주차반응도함께삭제한다() {
        CafeteriaMenu cafeteriaMenu = CafeteriaMenu.create(
                "2026-W08",
                LocalDate.of(2026, 2, 16),
                LocalDate.of(2026, 2, 20),
                Map.of("2026-02-16", Map.of("rollNoodles", List.of("우동"))),
                Map.of()
        );
        when(cafeteriaMenuRepository.findById("2026-W08")).thenReturn(Optional.of(cafeteriaMenu));

        cafeteriaMenuService.deleteMenu("2026-W08");

        verify(cafeteriaMenuReactionRepository).deleteByWeekId("2026-W08");
        verify(cafeteriaMenuRepository).delete(cafeteriaMenu);
    }

    @Test
    void createMenu_menus와MenuEntries가불일치하면_예외() {
        when(cafeteriaMenuRepository.existsById("2026-W08")).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> cafeteriaMenuService.createMenu(new CreateCafeteriaMenuRequest(
                        "2026-W08",
                        LocalDate.of(2026, 2, 16),
                        LocalDate.of(2026, 2, 20),
                        Map.of(
                                "2026-02-16",
                                Map.of("rollNoodles", List.of("우동"))
                        ),
                        Map.of(
                                "2026-02-16",
                                Map.of(
                                        "rollNoodles",
                                        List.of(new CafeteriaMenuEntryRequest("라면", List.of(), 3, 1))
                                )
                        )
                ))
        );

        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
        assertEquals("menus와 menuEntries의 메뉴명이 일치하지 않습니다.", exception.getMessage());
    }

    @Test
    void createMenu_주간동일메뉴메타데이터가다르면_예외() {
        when(cafeteriaMenuRepository.existsById("2026-W08")).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> cafeteriaMenuService.createMenu(new CreateCafeteriaMenuRequest(
                        "2026-W08",
                        LocalDate.of(2026, 2, 16),
                        LocalDate.of(2026, 2, 20),
                        null,
                        Map.of(
                                "2026-02-16",
                                Map.of(
                                        "rollNoodles",
                                        List.of(new CafeteriaMenuEntryRequest(
                                                "존슨부대찌개",
                                                List.of(new CafeteriaMenuBadgeRequest("TAKEOUT", "테이크아웃")),
                                                10,
                                                1
                                        ))
                                ),
                                "2026-02-17",
                                Map.of(
                                        "rollNoodles",
                                        List.of(new CafeteriaMenuEntryRequest(
                                                "존슨부대찌개",
                                                List.of(new CafeteriaMenuBadgeRequest("SPICY", "매운맛")),
                                                10,
                                                1
                                        ))
                                )
                        )
                ))
        );

        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("같은 주차에서 동일 카테고리의 동일 메뉴는 날짜별 메타데이터가 동일해야 합니다."));
        assertTrue(exception.getMessage().contains("category=rollNoodles"));
        assertTrue(exception.getMessage().contains("title=존슨부대찌개"));
    }

    @Test
    void createMenu_카테고리코드에점이포함되면_예외() {
        when(cafeteriaMenuRepository.existsById("2026-W08")).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> cafeteriaMenuService.createMenu(new CreateCafeteriaMenuRequest(
                        "2026-W08",
                        LocalDate.of(2026, 2, 16),
                        LocalDate.of(2026, 2, 20),
                        null,
                        Map.of(
                                "2026-02-16",
                                Map.of(
                                        "special.v1",
                                        List.of(new CafeteriaMenuEntryRequest("우동", List.of(), 0, 0))
                                )
                        )
                ))
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("menuEntries.category는 영문, 숫자, 밑줄(_), 하이픈(-)만 사용할 수 있습니다.", exception.getMessage());
    }

    @Test
    void getMenuByWeekId_기존행에menuEntries가없어도_기본메타데이터를응답한다() {
        when(cafeteriaMenuRepository.findById("2026-W08")).thenReturn(Optional.of(CafeteriaMenu.create(
                "2026-W08",
                LocalDate.of(2026, 2, 16),
                LocalDate.of(2026, 2, 20),
                Map.of(
                        "2026-02-16",
                        Map.of("rollNoodles", List.of("우동"))
                ),
                Map.of()
        )));

        CafeteriaMenuResponse response = cafeteriaMenuService.getMenuByWeekId("2026-W08");

        assertEquals("우동", response.menuEntries().get("2026-02-16").get("rollNoodles").get(0).title());
        assertEquals(0, response.menuEntries().get("2026-02-16").get("rollNoodles").get(0).likeCount());
        assertTrue(response.menuEntries().get("2026-02-16").get("fryRice").isEmpty());
    }

    @Test
    void getMenuByWeekId_실제반응집계와내반응을응답에주입한다() {
        CafeteriaMenu cafeteriaMenu = CafeteriaMenu.create(
                "2026-W08",
                LocalDate.of(2026, 2, 16),
                LocalDate.of(2026, 2, 20),
                Map.of("2026-02-16", Map.of("rollNoodles", List.of("존슨부대찌개"))),
                Map.of(
                        "2026-02-16",
                        Map.of(
                                "rollNoodles",
                                List.of(new CafeteriaMenuEntryMetadata(
                                        "존슨부대찌개",
                                        List.of(new CafeteriaMenuBadgeMetadata("TAKEOUT", "테이크아웃")),
                                        0,
                                        0
                                ))
                        )
                )
        );
        String menuId = CafeteriaMenuIdCodec.encode("2026-W08", "rollNoodles", "존슨부대찌개");

        when(cafeteriaMenuRepository.findById("2026-W08")).thenReturn(Optional.of(cafeteriaMenu));
        when(cafeteriaMenuReactionRepository.summarizeCounts("2026-W08", Set.of(menuId)))
                .thenReturn(List.of(countProjection(menuId, 7, 2)));
        when(cafeteriaMenuReactionRepository.findSelections("user-1", "2026-W08", Set.of(menuId)))
                .thenReturn(List.of(selectionProjection(menuId, CafeteriaMenuReactionType.LIKE)));

        CafeteriaMenuResponse response = cafeteriaMenuService.getMenuByWeekId("user-1", "2026-W08");

        CafeteriaMenuEntryResponse entry = response.menuEntries().get("2026-02-16").get("rollNoodles").get(0);
        assertEquals(menuId, entry.id());
        assertEquals(7, entry.likeCount());
        assertEquals(2, entry.dislikeCount());
        assertEquals(CafeteriaMenuReactionType.LIKE, entry.myReaction());
    }

    @Test
    void getMenuByWeekId_memberId가없으면_myReaction은null이다() {
        CafeteriaMenu cafeteriaMenu = CafeteriaMenu.create(
                "2026-W08",
                LocalDate.of(2026, 2, 16),
                LocalDate.of(2026, 2, 20),
                Map.of("2026-02-16", Map.of("rollNoodles", List.of("존슨부대찌개"))),
                Map.of()
        );
        String menuId = CafeteriaMenuIdCodec.encode("2026-W08", "rollNoodles", "존슨부대찌개");

        when(cafeteriaMenuRepository.findById("2026-W08")).thenReturn(Optional.of(cafeteriaMenu));
        when(cafeteriaMenuReactionRepository.summarizeCounts("2026-W08", Set.of(menuId)))
                .thenReturn(List.of(countProjection(menuId, 3, 1)));

        CafeteriaMenuResponse response = cafeteriaMenuService.getMenuByWeekId(null, "2026-W08");

        CafeteriaMenuEntryResponse entry = response.menuEntries().get("2026-02-16").get("rollNoodles").get(0);
        assertEquals(3, entry.likeCount());
        assertEquals(1, entry.dislikeCount());
        assertNull(entry.myReaction());
        verify(cafeteriaMenuReactionRepository, never()).findSelections(any(), any(), any());
    }

    private CafeteriaMenuReactionRepository.CafeteriaMenuReactionCountProjection countProjection(
            String menuId,
            long likeCount,
            long dislikeCount
    ) {
        return new CafeteriaMenuReactionRepository.CafeteriaMenuReactionCountProjection() {
            @Override
            public String getMenuId() {
                return menuId;
            }

            @Override
            public long getLikeCount() {
                return likeCount;
            }

            @Override
            public long getDislikeCount() {
                return dislikeCount;
            }
        };
    }

    private CafeteriaMenuReactionRepository.CafeteriaMenuReactionSelectionProjection selectionProjection(
            String menuId,
            CafeteriaMenuReactionType reactionType
    ) {
        return new CafeteriaMenuReactionRepository.CafeteriaMenuReactionSelectionProjection() {
            @Override
            public String getMenuId() {
                return menuId;
            }

            @Override
            public CafeteriaMenuReactionType getReaction() {
                return reactionType;
            }
        };
    }
}
