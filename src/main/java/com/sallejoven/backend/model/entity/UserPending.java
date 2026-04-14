// entity
package com.sallejoven.backend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(of = "uuid")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_pending")
public class UserPending {

    @Id
    @Column(name = "uuid", nullable=false, unique = true, updatable = false, columnDefinition = "uuid")
    private UUID uuid;

    @Column(nullable=false) private String name;
    @Column(name="last_name", nullable=false) private String lastName;
    @Column(nullable=false) private String dni;
    @Column private String phone;
    @Column(nullable=false) private String email;

    @Column(name="tshirt_size") private Integer tshirtSize;
    @Column(name="health_card_number") private String healthCardNumber;
    @Column private String intolerances;
    @Column(name="chronic_diseases") private String chronicDiseases;
    @Column private String city;
    @Column private String address;
    @Column(name="image_authorization") private Boolean imageAuthorization;
    @Temporal(TemporalType.DATE) @Column(name="birth_date") private Date birthDate;

    @Column private String roles;
    @Column(nullable=false) private String password;
    @Column private Integer gender;

    @Column(name="mother_full_name") private String motherFullName;
    @Column(name="father_full_name") private String fatherFullName;
    @Column(name="mother_email") private String motherEmail;
    @Column(name="father_email") private String fatherEmail;
    @Column(name="father_phone") private String fatherPhone;
    @Column(name="mother_phone") private String motherPhone;

    @Column(name="center_uuid") private UUID centerUuid;
    @Column(name="group_uuid") private UUID groupUuid;

    @Column(name="created_at", nullable=false) private LocalDateTime createdAt;

    @jakarta.persistence.PrePersist
    private void ensureUuid() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }

    public UUID getId() {
        return uuid;
    }

    public void setId(UUID id) {
        this.uuid = id;
    }
}
