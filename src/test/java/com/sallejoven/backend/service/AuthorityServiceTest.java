package com.sallejoven.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sallejoven.backend.model.entity.GroupSalle;
import com.sallejoven.backend.model.entity.UserGroup;
import com.sallejoven.backend.model.entity.UserSalle;
import com.sallejoven.backend.model.enums.Role;
import com.sallejoven.backend.repository.UserCenterRepository;
import com.sallejoven.backend.repository.UserGroupRepository;
import com.sallejoven.backend.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthorityServiceTest {

    @Mock AcademicStateService academicStateService;
    @Mock UserRepository userRepository;
    @Mock UserCenterRepository userCenterRepository;
    @Mock UserGroupRepository userGroupRepository;

    @InjectMocks AuthorityService authorityService;

    @Test
    void buildContextAuthorities_normalizesLegacyCatechistTypeAsAnimator() {
        UUID userUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();

        when(academicStateService.getVisibleYear()).thenReturn(2025);
        when(userCenterRepository.findByUser_UuidAndYearAndDeletedAtIsNull(userUuid, 2025)).thenReturn(List.of());
        when(userGroupRepository.findByUser_UuidAndYearAndDeletedAtIsNull(userUuid, 2025))
                .thenReturn(List.of(UserGroup.builder()
                        .group(GroupSalle.builder().uuid(groupUuid).build())
                        .userType(5)
                        .year(2025)
                        .build()));

        assertThat(authorityService.buildContextAuthorities(userUuid))
                .containsExactly("GROUP:" + groupUuid + ":ANIMATOR:2025");
    }

    @Test
    void computeDisplayRole_returnsAnimator_forLegacyCatechistType() {
        UUID userUuid = UUID.randomUUID();

        UserSalle user = new UserSalle();
        user.setUuid(userUuid);
        user.setIsAdmin(false);

        when(userRepository.findById(userUuid)).thenReturn(Optional.of(user));
        when(academicStateService.getVisibleYear()).thenReturn(2025);
        when(userCenterRepository.existsByUser_UuidAndYearAndDeletedAtIsNullAndUserType(userUuid, 2025, 3))
                .thenReturn(false);
        when(userCenterRepository.existsByUser_UuidAndYearAndDeletedAtIsNullAndUserType(userUuid, 2025, 2))
                .thenReturn(false);
        when(userGroupRepository.findByUser_UuidAndYearAndDeletedAtIsNull(userUuid, 2025))
                .thenReturn(List.of(UserGroup.builder()
                        .userType(5)
                        .year(2025)
                        .build()));

        assertThat(authorityService.computeDisplayRole(userUuid)).isEqualTo(Role.ANIMATOR);
    }
}
