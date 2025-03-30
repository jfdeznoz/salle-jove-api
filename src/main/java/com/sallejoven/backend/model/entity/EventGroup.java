package com.sallejoven.backend.model.entity;

import com.sallejoven.backend.model.ids.EventGroupId;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "event_group")
public class EventGroup {

    @EmbeddedId
    private EventGroupId id;

    @ManyToOne
    @MapsId("event")
    @JoinColumn(name = "event", nullable = false)
    private Event event;

    @ManyToOne
    @MapsId("groupSalle")
    @JoinColumn(name = "group_salle", nullable = false)
    private GroupSalle groupSalle;
}
