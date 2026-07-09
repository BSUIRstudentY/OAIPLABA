package com.vidvault.api.repo;

import com.vidvault.api.domain.Video;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VideoRepository extends JpaRepository<Video, UUID> {
    List<Video> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    List<Video> findByListedForSaleTrueOrderByListedAtDesc();
}
