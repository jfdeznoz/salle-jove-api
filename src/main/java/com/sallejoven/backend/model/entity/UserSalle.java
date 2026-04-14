package com.sallejoven.backend.model.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@EqualsAndHashCode(of = "uuid")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_salle")
public class UserSalle {

    @Id
    @Column(name = "uuid", nullable = false, unique = true, updatable = false, columnDefinition = "uuid")
    private UUID uuid;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String dni;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private Integer tshirtSize;

    @Column(nullable = false)
    private String healthCardNumber;

    @Column
    private String intolerances;

    @Column
    private String chronicDiseases;

    @Column
    private String city;

    @Column(nullable = false)
    private Boolean imageAuthorization;

    @Column(nullable = false)
    private Date birthDate;

    @Column
    private String address;

    @Column(nullable = false)
    private Boolean isAdmin;

    @ToString.Exclude
    private String password;

    @OneToMany(
            mappedBy = "user",
            fetch = FetchType.LAZY,
            cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @SQLRestriction("deleted_at IS NULL")
    private Set<UserGroup> groups = new HashSet<>();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private Integer gender;

    @Column(name = "mother_full_name")
    private String motherFullName;

    @Column(name = "father_full_name")
    private String fatherFullName;

    @Column(name = "mother_email")
    private String motherEmail;

    @Column(name = "father_email")
    private String fatherEmail;

    @Column(name = "father_phone")
    private String fatherPhone;

    @Column(name = "mother_phone")
    private String motherPhone;

    @PrePersist
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
