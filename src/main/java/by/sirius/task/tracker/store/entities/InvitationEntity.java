package by.sirius.task.tracker.store.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "invitations")
public class InvitationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "inviting_admin_id", referencedColumnName = "id")
    private UserEntity invitingAdmin;

    @ManyToOne
    @JoinColumn(name = "invited_user_id", referencedColumnName = "id")
    private UserEntity invitedUser;

    @ManyToOne
    @JoinColumn(name = "project_id", referencedColumnName = "id")
    private ProjectEntity project;

    @Enumerated(EnumType.STRING)
    private InvitationStatus status;

    @Builder.Default
    private Instant sentAt = Instant.now();
}
