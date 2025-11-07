package com.ead.gearup.model;

import com.ead.gearup.enums.ProjectUpdateType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "project_updates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectUpdate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;
    
    @Column(nullable = false, length = 2000)
    private String message;
    
    @Column(name = "completed_tasks")
    private Integer completedTasks;
    
    @Column(name = "total_tasks")
    private Integer totalTasks;
    
    @Column(name = "additional_cost")
    private Double additionalCost;
    
    @Column(name = "additional_cost_reason", length = 500)
    private String additionalCostReason;
    
    @Column(name = "estimated_completion_date")
    private LocalDate estimatedCompletionDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "update_type", nullable = false)
    private ProjectUpdateType updateType;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
