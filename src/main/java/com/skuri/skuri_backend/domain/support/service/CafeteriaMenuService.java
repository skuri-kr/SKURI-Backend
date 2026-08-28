package com.skuri.skuri_backend.domain.support.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.support.dto.request.CafeteriaMenuBadgeRequest;
import com.skuri.skuri_backend.domain.support.dto.request.CafeteriaMenuEntryRequest;
import com.skuri.skuri_backend.domain.support.dto.request.CreateCafeteriaMenuRequest;
import com.skuri.skuri_backend.domain.support.dto.request.UpdateCafeteriaMenuRequest;
import com.skuri.skuri_backend.domain.support.dto.response.CafeteriaMenuBadgeResponse;
import com.skuri.skuri_backend.domain.support.dto.response.CafeteriaMenuCategoryResponse;
import com.skuri.skuri_backend.domain.support.dto.response.CafeteriaMenuEntryResponse;
import com.skuri.skuri_backend.domain.support.dto.response.CafeteriaMenuResponse;
import com.skuri.skuri_backend.domain.support.entity.CafeteriaMenu;
import com.skuri.skuri_backend.domain.support.exception.CafeteriaMenuNotFoundException;
import com.skuri.skuri_backend.domain.support.model.CafeteriaMenuIdCodec;
import com.skuri.skuri_backend.domain.support.model.CafeteriaMenuBadgeMetadata;
import com.skuri.skuri_backend.domain.support.model.CafeteriaMenuEntryMetadata;
import com.skuri.skuri_backend.domain.support.model.CafeteriaMenuReactionType;
import com.skuri.skuri_backend.domain.support.repository.CafeteriaMenuReactionRepository;
import com.skuri.skuri_backend.domain.support.repository.CafeteriaMenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CafeteriaMenuService {

    private static final String CATEGORY_CODE_PATTERN = "^[A-Za-z0-9_-]+$";
    private static final Map<String, String> KNOWN_CATEGORY_LABELS = Map.of(
            "rollNoodles", "Roll & Noodles",
            "theBab", "The bab",
            "fryRice", "Fry & Rice"
    );
    private static final List<String> KNOWN_CATEGORY_ORDER = List.of("rollNoodles", "theBab", "fryRice");

    private final CafeteriaMenuRepository cafeteriaMenuRepository;
    private final CafeteriaMenuReactionRepository cafeteriaMenuReactionRepository;

    @Transactional(readOnly = true)
    public CafeteriaMenuResponse getCurrentWeekMenu(LocalDate date) {
        return getCurrentWeekMenu(null, date);
    }

    @Transactional(readOnly = true)
    public CafeteriaMenuResponse getCurrentWeekMenuWithoutReactions(LocalDate date) {
        CafeteriaMenu cafeteriaMenu = cafeteriaMenuRepository.findById(toWeekId(date))
                .orElseThrow(CafeteriaMenuNotFoundException::new);
        return toResponseWithoutReactions(cafeteriaMenu);
    }

    @Transactional(readOnly = true)
    public CafeteriaMenuResponse getCurrentWeekMenu(String memberId, LocalDate date) {
        return getMenuByWeekId(memberId, toWeekId(date));
    }

    @Transactional(readOnly = true)
    public CafeteriaMenuResponse getMenuByWeekId(String weekId) {
        return getMenuByWeekId(null, weekId);
    }

    @Transactional(readOnly = true)
    public CafeteriaMenuResponse getMenuByWeekId(String memberId, String weekId) {
        CafeteriaMenu cafeteriaMenu = cafeteriaMenuRepository.findById(normalizeWeekId(weekId))
                .orElseThrow(CafeteriaMenuNotFoundException::new);
        return toResponse(cafeteriaMenu, memberId);
    }

    @Transactional
    public CafeteriaMenuResponse createMenu(CreateCafeteriaMenuRequest request) {
        String normalizedWeekId = normalizeWeekId(request.weekId());
        validateWeekRange(normalizedWeekId, request.weekStart(), request.weekEnd());
        if (cafeteriaMenuRepository.existsById(normalizedWeekId)) {
            throw new BusinessException(ErrorCode.CAFETERIA_MENU_ALREADY_EXISTS);
        }

        NormalizedCafeteriaMenuPayload payload = normalizePayload(request.menus(), request.menuEntries());

        CafeteriaMenu cafeteriaMenu = cafeteriaMenuRepository.save(CafeteriaMenu.create(
                normalizedWeekId,
                request.weekStart(),
                request.weekEnd(),
                payload.menus(),
                payload.menuEntries()
        ));
        return toResponse(cafeteriaMenu);
    }

    @Transactional
    public CafeteriaMenuResponse updateMenu(String weekId, UpdateCafeteriaMenuRequest request) {
        String normalizedWeekId = normalizeWeekId(weekId);
        validateWeekRange(normalizedWeekId, request.weekStart(), request.weekEnd());
        CafeteriaMenu cafeteriaMenu = cafeteriaMenuRepository.findById(normalizedWeekId)
                .orElseThrow(CafeteriaMenuNotFoundException::new);

        NormalizedCafeteriaMenuPayload payload = normalizePayload(request.menus(), request.menuEntries());
        cafeteriaMenu.update(request.weekStart(), request.weekEnd(), payload.menus(), payload.menuEntries());
        purgeObsoleteReactions(normalizedWeekId, payload.menuEntries());
        return toResponse(cafeteriaMenu);
    }

    @Transactional
    public void deleteMenu(String weekId) {
        CafeteriaMenu cafeteriaMenu = cafeteriaMenuRepository.findById(normalizeWeekId(weekId))
                .orElseThrow(CafeteriaMenuNotFoundException::new);
        cafeteriaMenuReactionRepository.deleteByWeekId(cafeteriaMenu.getWeekId());
        cafeteriaMenuRepository.delete(cafeteriaMenu);
    }

    public CafeteriaMenuResponse toResponse(CafeteriaMenu cafeteriaMenu) {
        return toResponse(cafeteriaMenu, null);
    }

    public CafeteriaMenuResponse toResponse(CafeteriaMenu cafeteriaMenu, String memberId) {
        Map<String, Map<String, List<CafeteriaMenuEntryMetadata>>> normalizedEntries =
                cafeteriaMenu.getMenuEntries().isEmpty()
                        ? synthesizeMenuEntries(cafeteriaMenu.getMenus())
                        : completeCategoryEntries(cafeteriaMenu.getMenuEntries());
        Map<String, CafeteriaReactionSummary> reactionSummaries = buildReactionSummaries(
                cafeteriaMenu.getWeekId(),
                collectMenuIds(cafeteriaMenu.getWeekId(), normalizedEntries),
                memberId
        );

        return toResponse(cafeteriaMenu, normalizedEntries, reactionSummaries);
    }

    private CafeteriaMenuResponse toResponseWithoutReactions(CafeteriaMenu cafeteriaMenu) {
        Map<String, Map<String, List<CafeteriaMenuEntryMetadata>>> normalizedEntries =
                cafeteriaMenu.getMenuEntries().isEmpty()
                        ? synthesizeMenuEntries(cafeteriaMenu.getMenus())
                        : completeCategoryEntries(cafeteriaMenu.getMenuEntries());
        return toResponse(cafeteriaMenu, normalizedEntries, Map.of());
    }

    private CafeteriaMenuResponse toResponse(
            CafeteriaMenu cafeteriaMenu,
            Map<String, Map<String, List<CafeteriaMenuEntryMetadata>>> normalizedEntries,
            Map<String, CafeteriaReactionSummary> reactionSummaries
    ) {
        return new CafeteriaMenuResponse(
                cafeteriaMenu.getWeekId(),
                cafeteriaMenu.getWeekStart(),
                cafeteriaMenu.getWeekEnd(),
                deepCopyMenus(cafeteriaMenu.getMenus()),
                buildCategoryResponses(cafeteriaMenu.getMenus(), normalizedEntries),
                toResponseMenuEntries(cafeteriaMenu.getWeekId(), normalizedEntries, reactionSummaries)
        );
    }

    private void validateWeekRange(String weekId, LocalDate weekStart, LocalDate weekEnd) {
        if (weekEnd.isBefore(weekStart)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "weekEnd는 weekStart보다 빠를 수 없습니다.");
        }
        if (!weekId.equals(toWeekId(weekStart))) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "weekId와 weekStart가 일치하지 않습니다.");
        }
    }

    private String normalizeWeekId(String weekId) {
        if (!StringUtils.hasText(weekId) || !weekId.matches("^\\d{4}-W\\d{2}$")) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "weekId 형식은 yyyy-Www 이어야 합니다.");
        }
        return weekId.trim().toUpperCase(Locale.ROOT);
    }

    public static String toWeekId(LocalDate date) {
        WeekFields weekFields = WeekFields.ISO;
        return "%04d-W%02d".formatted(date.get(weekFields.weekBasedYear()), date.get(weekFields.weekOfWeekBasedYear()));
    }

    private NormalizedCafeteriaMenuPayload normalizePayload(
            Map<String, Map<String, List<String>>> menus,
            Map<String, Map<String, List<CafeteriaMenuEntryRequest>>> menuEntries
    ) {
        Map<String, Map<String, List<String>>> normalizedMenus = canonicalizeMenus(normalizeMenus(menus));
        Map<String, Map<String, List<CafeteriaMenuEntryMetadata>>> normalizedMenuEntries =
                normalizeMenuEntries(menuEntries);

        boolean hasMenus = !normalizedMenus.isEmpty();
        boolean hasMenuEntries = !normalizedMenuEntries.isEmpty();
        if (!hasMenus && !hasMenuEntries) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "menus 또는 menuEntries 중 하나는 비어 있을 수 없습니다.");
        }

        if (hasMenuEntries) {
            validateWeeklyMenuEntryConsistency(normalizedMenuEntries);
            Map<String, Map<String, List<String>>> menusFromEntries =
                    canonicalizeMenus(extractMenus(normalizedMenuEntries));
            if (hasMenus && !Objects.equals(normalizedMenus, menusFromEntries)) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "menus와 menuEntries의 메뉴명이 일치하지 않습니다.");
            }
            return new NormalizedCafeteriaMenuPayload(menusFromEntries, completeCategoryEntries(normalizedMenuEntries));
        }

        return new NormalizedCafeteriaMenuPayload(normalizedMenus, synthesizeMenuEntries(normalizedMenus));
    }

    private Map<String, Map<String, List<String>>> normalizeMenus(Map<String, Map<String, List<String>>> source) {
        Map<String, Map<String, List<String>>> result = new LinkedHashMap<>();
        if (source == null) {
            return result;
        }
        source.forEach((date, restaurants) -> {
            String normalizedDate = normalizeRequiredText("menus.date", date);
            Map<String, List<String>> normalizedRestaurants = new LinkedHashMap<>();
            if (restaurants != null) {
                restaurants.forEach((restaurant, items) -> normalizedRestaurants.put(
                        normalizeCategoryCode("menus.category", restaurant),
                        normalizeMenuTitles(items)
                ));
            }
            result.put(normalizedDate, normalizedRestaurants);
        });
        return result;
    }

    private Map<String, Map<String, List<CafeteriaMenuEntryMetadata>>> normalizeMenuEntries(
            Map<String, Map<String, List<CafeteriaMenuEntryRequest>>> source
    ) {
        Map<String, Map<String, List<CafeteriaMenuEntryMetadata>>> result = new LinkedHashMap<>();
        if (source == null) {
            return result;
        }
        source.forEach((date, categories) -> {
            String normalizedDate = normalizeRequiredText("menuEntries.date", date);
            Map<String, List<CafeteriaMenuEntryMetadata>> normalizedCategories = new LinkedHashMap<>();
            if (categories != null) {
                categories.forEach((category, items) -> normalizedCategories.put(
                        normalizeCategoryCode("menuEntries.category", category),
                        normalizeEntryItems(items)
                ));
            }
            result.put(normalizedDate, normalizedCategories);
        });
        return result;
    }

    private List<CafeteriaMenuEntryMetadata> normalizeEntryItems(List<CafeteriaMenuEntryRequest> source) {
        if (source == null) {
            return List.of();
        }
        List<CafeteriaMenuEntryMetadata> items = new ArrayList<>();
        for (CafeteriaMenuEntryRequest request : source) {
            if (request == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "menuEntries에 null 항목을 포함할 수 없습니다.");
            }
            items.add(new CafeteriaMenuEntryMetadata(
                    normalizeRequiredText("menuEntries.title", request.title()),
                    normalizeBadgeMetadata(request.badges()),
                    normalizeReactionCount("menuEntries.likeCount", request.likeCount()),
                    normalizeReactionCount("menuEntries.dislikeCount", request.dislikeCount())
            ));
        }
        return List.copyOf(items);
    }

    private List<CafeteriaMenuBadgeMetadata> normalizeBadgeMetadata(List<CafeteriaMenuBadgeRequest> source) {
        if (source == null) {
            return List.of();
        }
        List<CafeteriaMenuBadgeMetadata> badges = new ArrayList<>();
        for (CafeteriaMenuBadgeRequest badge : source) {
            if (badge == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "menuEntries.badges에 null 항목을 포함할 수 없습니다.");
            }
            String label = normalizeRequiredText("menuEntries.badges.label", badge.label());
            badges.add(new CafeteriaMenuBadgeMetadata(normalizeBadgeCode(badge.code(), label), label));
        }
        return List.copyOf(badges);
    }

    private int normalizeReactionCount(String fieldName, Integer value) {
        if (value != null && value < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, fieldName + "는 0 이상이어야 합니다.");
        }
        return 0;
    }

    private String normalizeBadgeCode(String code, String label) {
        String base = StringUtils.hasText(code) ? code : label;
        String normalized = base.trim()
                .replaceAll("\\s+", "_")
                .replaceAll("[^A-Za-z0-9가-힣_\\-]", "")
                .toUpperCase(Locale.ROOT);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "menuEntries.badges.code는 비어 있을 수 없습니다.");
        }
        return normalized;
    }

    private String normalizeRequiredText(String fieldName, String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, fieldName + "는 비어 있을 수 없습니다.");
        }
        return value.trim();
    }

    private String normalizeCategoryCode(String fieldName, String value) {
        String normalized = normalizeRequiredText(fieldName, value);
        if (!normalized.matches(CATEGORY_CODE_PATTERN)) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    fieldName + "는 영문, 숫자, 밑줄(_), 하이픈(-)만 사용할 수 있습니다."
            );
        }
        return normalized;
    }

    private List<String> normalizeMenuTitles(List<String> items) {
        if (items == null) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>();
        for (String item : items) {
            normalized.add(normalizeRequiredText("menus.title", item));
        }
        return List.copyOf(normalized);
    }

    private Map<String, Map<String, List<String>>> canonicalizeMenus(
            Map<String, Map<String, List<String>>> menus
    ) {
        Map<String, Map<String, List<String>>> canonical = new LinkedHashMap<>();
        menus.forEach((date, categories) -> {
            Map<String, List<String>> filteredCategories = new LinkedHashMap<>();
            categories.forEach((category, items) -> {
                if (!items.isEmpty()) {
                    filteredCategories.put(category, items);
                }
            });
            canonical.put(date, filteredCategories);
        });
        return canonical;
    }

    private Map<String, Map<String, List<CafeteriaMenuEntryMetadata>>> synthesizeMenuEntries(
            Map<String, Map<String, List<String>>> menus
    ) {
        Map<String, Map<String, List<CafeteriaMenuEntryMetadata>>> result = new LinkedHashMap<>();
        menus.forEach((date, categories) -> {
            Map<String, List<CafeteriaMenuEntryMetadata>> dateEntries = new LinkedHashMap<>();
            categories.forEach((category, items) -> dateEntries.put(
                    category,
                    items.stream()
                            .map(title -> new CafeteriaMenuEntryMetadata(title, List.of(), 0, 0))
                            .toList()
            ));
            result.put(date, dateEntries);
        });
        return completeCategoryEntries(result);
    }

    private Map<String, Map<String, List<CafeteriaMenuEntryMetadata>>> completeCategoryEntries(
            Map<String, Map<String, List<CafeteriaMenuEntryMetadata>>> source
    ) {
        Map<String, Map<String, List<CafeteriaMenuEntryMetadata>>> completed = new LinkedHashMap<>();
        source.forEach((date, categories) -> {
            Map<String, List<CafeteriaMenuEntryMetadata>> orderedCategories = new LinkedHashMap<>();
            for (String knownCategory : KNOWN_CATEGORY_ORDER) {
                orderedCategories.put(knownCategory, List.copyOf(categories.getOrDefault(knownCategory, List.of())));
            }
            categories.forEach((category, items) -> {
                if (!orderedCategories.containsKey(category)) {
                    orderedCategories.put(category, List.copyOf(items));
                }
            });
            completed.put(date, orderedCategories);
        });
        return completed;
    }

    private void validateWeeklyMenuEntryConsistency(
            Map<String, Map<String, List<CafeteriaMenuEntryMetadata>>> menuEntries
    ) {
        Map<String, CafeteriaMenuEntryMetadata> weeklyMetadata = new LinkedHashMap<>();
        Map<String, String> firstSeenDate = new LinkedHashMap<>();

        menuEntries.forEach((date, categories) -> categories.forEach((category, items) -> {
            for (CafeteriaMenuEntryMetadata item : items) {
                String key = category + "\u0000" + item.title();
                CafeteriaMenuEntryMetadata existing = weeklyMetadata.putIfAbsent(key, item);
                if (existing == null) {
                    firstSeenDate.put(key, date);
                    continue;
                }
                if (!existing.equals(item)) {
                    throw new BusinessException(
                            ErrorCode.INVALID_REQUEST,
                            "같은 주차에서 동일 카테고리의 동일 메뉴는 날짜별 메타데이터가 동일해야 합니다. "
                                    + "category=%s, title=%s, firstDate=%s, date=%s".formatted(
                                    category,
                                    item.title(),
                                    firstSeenDate.get(key),
                                    date
                            )
                    );
                }
            }
        }));
    }

    private Map<String, Map<String, List<String>>> extractMenus(
            Map<String, Map<String, List<CafeteriaMenuEntryMetadata>>> menuEntries
    ) {
        Map<String, Map<String, List<String>>> result = new LinkedHashMap<>();
        menuEntries.forEach((date, categories) -> {
            Map<String, List<String>> categoryTitles = new LinkedHashMap<>();
            categories.forEach((category, items) -> categoryTitles.put(
                    category,
                    items.stream().map(CafeteriaMenuEntryMetadata::title).toList()
            ));
            result.put(date, categoryTitles);
        });
        return result;
    }

    private List<CafeteriaMenuCategoryResponse> buildCategoryResponses(
            Map<String, Map<String, List<String>>> menus,
            Map<String, Map<String, List<CafeteriaMenuEntryMetadata>>> menuEntries
    ) {
        Set<String> categoryCodes = new LinkedHashSet<>(KNOWN_CATEGORY_ORDER);
        collectCategoryCodes(categoryCodes, menus);
        collectCategoryCodes(categoryCodes, menuEntries);
        return categoryCodes.stream()
                .map(code -> new CafeteriaMenuCategoryResponse(code, KNOWN_CATEGORY_LABELS.getOrDefault(code, code)))
                .toList();
    }

    private void collectCategoryCodes(Set<String> target, Map<String, ? extends Map<String, ?>> source) {
        source.values().forEach(categories -> target.addAll(categories.keySet()));
    }

    private Set<String> collectMenuIds(
            String weekId,
            Map<String, Map<String, List<CafeteriaMenuEntryMetadata>>> source
    ) {
        Set<String> menuIds = new LinkedHashSet<>();
        source.values().forEach(categories -> categories.forEach((category, items) ->
                items.forEach(item -> menuIds.add(CafeteriaMenuIdCodec.encode(weekId, category, item.title())))
        ));
        return menuIds;
    }

    private Map<String, CafeteriaReactionSummary> buildReactionSummaries(
            String weekId,
            Set<String> menuIds,
            String memberId
    ) {
        if (menuIds.isEmpty()) {
            return Map.of();
        }

        Map<String, CafeteriaReactionSummary> summaries = new LinkedHashMap<>();
        List<CafeteriaMenuReactionRepository.CafeteriaMenuReactionCountProjection> countResults =
                cafeteriaMenuReactionRepository.summarizeCounts(weekId, menuIds);
        if (countResults != null) {
            countResults.forEach(summary ->
                    summaries.put(
                            summary.getMenuId(),
                            new CafeteriaReactionSummary(
                                    Math.toIntExact(summary.getLikeCount()),
                                    Math.toIntExact(summary.getDislikeCount()),
                                    null
                            )
                    )
            );
        }

        if (memberId != null) {
            List<CafeteriaMenuReactionRepository.CafeteriaMenuReactionSelectionProjection> selections =
                    cafeteriaMenuReactionRepository.findSelections(memberId, weekId, menuIds);
            if (selections != null) {
                selections.forEach(selection -> {
                    CafeteriaReactionSummary summary = summaries.getOrDefault(selection.getMenuId(), CafeteriaReactionSummary.empty());
                    summaries.put(
                            selection.getMenuId(),
                            new CafeteriaReactionSummary(summary.likeCount(), summary.dislikeCount(), selection.getReaction())
                    );
                });
            }
        }

        return summaries;
    }

    private void purgeObsoleteReactions(
            String weekId,
            Map<String, Map<String, List<CafeteriaMenuEntryMetadata>>> menuEntries
    ) {
        Set<String> activeMenuIds = collectMenuIds(weekId, menuEntries);
        if (activeMenuIds.isEmpty()) {
            cafeteriaMenuReactionRepository.deleteByWeekId(weekId);
            return;
        }
        cafeteriaMenuReactionRepository.deleteObsoleteReactions(weekId, activeMenuIds);
    }

    private Map<String, Map<String, List<CafeteriaMenuEntryResponse>>> toResponseMenuEntries(
            String weekId,
            Map<String, Map<String, List<CafeteriaMenuEntryMetadata>>> source,
            Map<String, CafeteriaReactionSummary> reactionSummaries
    ) {
        Map<String, Map<String, List<CafeteriaMenuEntryResponse>>> result = new LinkedHashMap<>();
        source.forEach((date, categories) -> {
            Map<String, List<CafeteriaMenuEntryResponse>> responseCategories = new LinkedHashMap<>();
            categories.forEach((category, items) -> responseCategories.put(
                    category,
                    toResponseItems(weekId, category, items, reactionSummaries)
            ));
            result.put(date, responseCategories);
        });
        return result;
    }

    private List<CafeteriaMenuEntryResponse> toResponseItems(
            String weekId,
            String category,
            List<CafeteriaMenuEntryMetadata> items,
            Map<String, CafeteriaReactionSummary> reactionSummaries
    ) {
        List<CafeteriaMenuEntryResponse> responses = new ArrayList<>();
        for (CafeteriaMenuEntryMetadata item : items) {
            String id = CafeteriaMenuIdCodec.encode(weekId, category, item.title());
            CafeteriaReactionSummary reactionSummary = reactionSummaries.getOrDefault(id, CafeteriaReactionSummary.empty());
            responses.add(new CafeteriaMenuEntryResponse(
                    id,
                    item.title(),
                    item.badges().stream()
                            .map(badge -> new CafeteriaMenuBadgeResponse(badge.code(), badge.label()))
                            .toList(),
                    reactionSummary.likeCount(),
                    reactionSummary.dislikeCount(),
                    reactionSummary.myReaction()
            ));
        }
        return List.copyOf(responses);
    }

    private Map<String, Map<String, List<String>>> deepCopyMenus(Map<String, Map<String, List<String>>> source) {
        Map<String, Map<String, List<String>>> copied = new LinkedHashMap<>();
        source.forEach((date, restaurants) -> {
            Map<String, List<String>> copiedRestaurants = new LinkedHashMap<>();
            restaurants.forEach((restaurant, items) -> copiedRestaurants.put(restaurant, List.copyOf(items)));
            copied.put(date, copiedRestaurants);
        });
        return copied;
    }

    private record NormalizedCafeteriaMenuPayload(
            Map<String, Map<String, List<String>>> menus,
            Map<String, Map<String, List<CafeteriaMenuEntryMetadata>>> menuEntries
    ) {
    }

    private record CafeteriaReactionSummary(
            int likeCount,
            int dislikeCount,
            CafeteriaMenuReactionType myReaction
    ) {
        private static CafeteriaReactionSummary empty() {
            return new CafeteriaReactionSummary(0, 0, null);
        }
    }
}
