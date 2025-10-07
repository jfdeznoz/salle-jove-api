// entity
package com.sallejoven.backend.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_pending")
public class UserPending {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(name="center_id") private Long centerId;
    @Column(name="group_id")  private Long groupId;

    @Column(name="created_at", nullable=false) private LocalDateTime createdAt;
}