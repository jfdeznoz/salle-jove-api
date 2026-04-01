package com.sallejoven.backend.service;

import com.sallejoven.backend.errors.SalleException;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.WeeklySession;
import com.sallejoven.backend.model.entity.WeeklySessionUser;
import com.sallejoven.backend.model.enums.ErrorCodes;
import com.sallejoven.backend.model.requestDto.AttendanceUpdateDto;
import com.sallejoven.backend.repository.WeeklySessionUserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WeeklySessionUserService {

    private final WeeklySessionUserRepository weeklySessionUserRepository;

    public void saveAll(List<WeeklySessionUser> users) {
        weeklySessionUserRepository.saveAll(users);
    }

    public void save(WeeklySessionUser user) {
        weeklySessionUserRepository.save(user);
    }

    public Optional<WeeklySessionUser> findById(Long id) {
        return weeklySessionUserRepository.findById(id);
    }

    public List<WeeklySessionUser> findBySessionIdAndGroupId(Long sessionId, Long groupId) {
        return weeklySessionUserRepository.findBySessionIdAndGroupId(sessionId, groupId);
    }

    public List<WeeklySessionUser> findBySessionIdOrdered(Long sessionId) {
        return weeklySessionUserRepository.findBySessionIdOrdered(sessionId);
    }

    @Transactional
    public void softDeleteBySessionId(Long sessionId) {
        weeklySessionUserRepository.softDeleteBySessionId(sessionId);
    }

    @Transactional
    public int softDeleteBySessionIdAndUserGroupIds(Long sessionId, Collection<Long> userGroupIds) {
        if (userGroupIds == null || userGroupIds.isEmpty()) return 0;
        return weeklySessionUserRepository.softDeleteBySessionIdAndUserGroupIds(sessionId, userGroupIds);
    }

    /** Asigna un único UserGroup a una sesión semanal. */
    @Transactional
    public WeeklySessionUser assignSessionToUserGroup(WeeklySession session, UserGroup userGroup) {
        WeeklySessionUser wsu = WeeklySessionUser.builder()
                .weeklySession(session)
                .userGroup(userGroup)
                .status(0) // Default: no asiste
                .build();
        return weeklySessionUserRepository.save(wsu);
    }

    @Transactional
    public void assignSessionToUserGroups(WeeklySession session, Collection<UserGroup> userGroups) {
        if (userGroups == null || userGroups.isEmpty()) return;

        List<Long> already = weeklySessionUserRepository.findUserGroupIdsBySession(session.getId());

        List<WeeklySessionUser> toInsert = userGroups.stream()
                .filter(ug -> ug != null && ug.getId() != null && !already.contains(ug.getId()))
                .map(ug -> assignSessionToUserGroup(session, ug))
                .toList();

        if (!toInsert.isEmpty()) {
            saveAll(toInsert);
        }
    }

    @Transactional
    public int updateAttendanceForUserInGroup(Long sessionId, Long userId, Long groupId, int status) {
        return weeklySessionUserRepository.updateStatusBySessionUserAndGroup(sessionId, userId, groupId, status);
    }

    @Transactional
    public void updateParticipantsAttendance(Long sessionId, List<AttendanceUpdateDto> updates, Long groupId)
             {

        if (groupId == null) throw new SalleException(ErrorCodes.GROUP_NOT_FOUND);

        for (AttendanceUpdateDto dto : updates) {
            dto.validate();
            Long userId = dto.getUserId();
            if (userId == null) throw new SalleException(ErrorCodes.USER_NOT_FOUND);

            int updated = updateAttendanceForUserInGroup(
                    sessionId, userId, groupId, dto.getAttends());

            if (updated == 0) {
                throw new SalleException(ErrorCodes.USER_NOT_FOUND);
            }
        }
    }

    public void softDeleteByUserGroupIds(List<Long> userGroupIds, LocalDateTime when) {
        weeklySessionUserRepository.softDeleteByUserGroupIdIn(userGroupIds, when);
    }

    public void softDeleteByUserGroupIds(List<Long> userGroupIds) {
        LocalDateTime now = LocalDateTime.now();
        weeklySessionUserRepository.softDeleteByUserGroupIdIn(userGroupIds, now);
    }
}

