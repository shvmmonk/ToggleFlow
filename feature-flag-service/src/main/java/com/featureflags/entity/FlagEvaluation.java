package com.featureflags.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "flag_evaluations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlagEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flag_id", nullable = false)
    private FeatureFlag flag;

    @Column(name = "user_identifier", nullable = false)
    private String userIdentifier;

    @Column(nullable = false)
    private Boolean result;

    @Column(name = "evaluated_at", nullable = false, updatable = false)
    private LocalDateTime evaluatedAt;

    @PrePersist
    protected void onCreate() {
        if (evaluatedAt == null) {
            evaluatedAt = LocalDateTime.now();
        }
    }
}
