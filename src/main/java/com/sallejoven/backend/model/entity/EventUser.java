package com.sallejoven.backend.model.entity;

import com.sallejoven.backend.model.ids.EventUserId;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "event_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventUser {

    @EmbeddedId
    private EventUserId id;

    @ManyToOne
    @MapsId("event")
    @JoinColumn(name = "event", nullable = false)
    private Event event;

    @ManyToOne
    @MapsId("userSalle")
    @JoinColumn(name = "user_salle", nullable = false)
    private UserSalle user;

    @Column(nullable = false)
    private int status;
}
