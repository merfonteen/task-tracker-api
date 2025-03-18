package by.sirius.task.tracker.store.repositories;

import by.sirius.task.tracker.store.entities.InvitationStatus;
import by.sirius.task.tracker.store.entities.ProjectEntity;
import by.sirius.task.tracker.store.entities.UserEntity;
import by.sirius.task.tracker.store.entities.InvitationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<InvitationEntity, Long> {
    List<InvitationEntity> findAllByInvitedUser_username(String username);

    Optional<InvitationEntity> findByInvitedUserAndProjectAndStatus(UserEntity invitedUser, ProjectEntity project, InvitationStatus status);
}