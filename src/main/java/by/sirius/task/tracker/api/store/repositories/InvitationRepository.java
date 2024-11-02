package by.sirius.task.tracker.api.store.repositories;

import by.sirius.task.tracker.api.store.entities.InvitationStatus;
import by.sirius.task.tracker.api.store.entities.ProjectEntity;
import by.sirius.task.tracker.api.store.entities.UserEntity;
import by.sirius.task.tracker.api.store.entities.InvitationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<InvitationEntity, Long> {
    List<InvitationEntity> findAllByInvitedUserAndStatus(UserEntity user, InvitationStatus status);
   List<InvitationEntity> findAllByInvitedUser_username(String username);
    Optional<InvitationEntity> findByInvitedUserAndProjectAndStatus(UserEntity invitedUser, ProjectEntity project, InvitationStatus status);
}