package fr.umontpellier.bloomcycle.repository;

import fr.umontpellier.bloomcycle.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByOwnerId(Long ownerId);

    List<Project> findByOwnerUsername(String username);

    Optional<Project> findByIdAndOwnerId(Long projectId, Long ownerId);

    Optional<Project> findByIdAndOwnerUsername(Long projectId, String username);
}
