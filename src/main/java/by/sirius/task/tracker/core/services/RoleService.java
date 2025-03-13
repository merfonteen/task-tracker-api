package by.sirius.task.tracker.core.services;

import by.sirius.task.tracker.store.entities.RoleEntity;
import by.sirius.task.tracker.store.repositories.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleEntity getUserRole() {
        return roleRepository.findByName("ROLE_USER").get();
    }

    public RoleEntity getAdminRole() {
        return roleRepository.findByName("ROLE_ADMIN").get(); }
}
