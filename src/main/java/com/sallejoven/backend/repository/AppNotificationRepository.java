package com.sallejoven.backend.repository;

import com.sallejoven.backend.model.entity.AppNotification;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AppNotificationRepository extends JpaRepository<AppNotification, UUID> {

    @Query("""
        select notification
        from AppNotification notification
        where notification.recipientUser.uuid = :userUuid
          and notification.deletedAt is null
        order by notification.createdAt desc
    """)
    Page<AppNotification> findPagedByRecipient(@Param("userUuid") UUID userUuid, Pageable pageable);

    @Query("""
        select count(notification)
        from AppNotification notification
        where notification.recipientUser.uuid = :userUuid
          and notification.deletedAt is null
          and notification.readAt is null
    """)
    long countUnreadByRecipient(@Param("userUuid") UUID userUuid);

    @Query("""
        select notification
        from AppNotification notification
        where notification.recipientUser.uuid = :userUuid
          and notification.deletedAt is null
          and notification.readAt is null
        order by notification.createdAt desc
    """)
    Page<AppNotification> findUnreadByRecipient(@Param("userUuid") UUID userUuid, Pageable pageable);

    @Query("""
        select notification
        from AppNotification notification
        where notification.uuid = :notificationUuid
          and notification.recipientUser.uuid = :userUuid
          and notification.deletedAt is null
    """)
    java.util.Optional<AppNotification> findOwnedByUuid(@Param("notificationUuid") UUID notificationUuid,
                                                         @Param("userUuid") UUID userUuid);

    @Modifying
    @Query("""
        update AppNotification notification
           set notification.readAt = :when
         where notification.recipientUser.uuid = :userUuid
           and notification.deletedAt is null
           and notification.readAt is null
    """)
    int markAllAsRead(@Param("userUuid") UUID userUuid, @Param("when") LocalDateTime when);
}
