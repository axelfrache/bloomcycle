package fr.umontpellier.bloomcycle.repository;

import fr.umontpellier.bloomcycle.model.File;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<File, Long> {
}