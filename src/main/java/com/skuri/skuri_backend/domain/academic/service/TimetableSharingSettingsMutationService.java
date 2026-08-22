package com.skuri.skuri_backend.domain.academic.service;

import com.skuri.skuri_backend.domain.academic.dto.request.UpdateTimetableSharingSettingsRequest;
import com.skuri.skuri_backend.domain.academic.entity.TimetableSharingSetting;
import com.skuri.skuri_backend.domain.academic.repository.TimetableSharingSettingRepository;
import com.skuri.skuri_backend.domain.friend.service.FriendMemberPairLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시간표 공유 기본값 변경을 전담한다.
 *
 * <p>변경 후 설정 조회는 호출자가 이 트랜잭션이 커밋된 뒤 별도로 수행한다. 친구 목록 조회는
 * FriendProfile lazy provisioning을 수행할 수 있으므로, 회원 행 잠금을 가진 상태에서 함께
 * 실행하면 안 된다.</p>
 */
@Service
@RequiredArgsConstructor
class TimetableSharingSettingsMutationService {

    private final TimetableSharingSettingRepository timetableSharingSettingRepository;
    private final FriendMemberPairLockService pairLockService;

    @Transactional
    public void updateDefaultScope(
            String ownerMemberId,
            UpdateTimetableSharingSettingsRequest request
    ) {
        pairLockService.lockActiveMember(ownerMemberId);
        timetableSharingSettingRepository.findById(ownerMemberId)
                .ifPresentOrElse(
                        setting -> setting.updateDefaultScope(request.defaultScope()),
                        () -> timetableSharingSettingRepository.save(
                                TimetableSharingSetting.create(ownerMemberId, request.defaultScope())
                        )
                );
    }
}
