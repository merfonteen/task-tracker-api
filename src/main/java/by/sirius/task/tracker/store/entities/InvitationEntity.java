package by.sirius.task.tracker.store.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(!(o instanceof InvitationEntity)) return false;
        InvitationEntity that = (InvitationEntity) o;
        return Objects.equals(that.id, id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
