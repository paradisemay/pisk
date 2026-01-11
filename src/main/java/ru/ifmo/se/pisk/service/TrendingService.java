package ru.ifmo.se.pisk.service;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ifmo.se.pisk.domain.entity.TrackEntity;
import ru.ifmo.se.pisk.repository.TrackRepository;
import ru.ifmo.se.pisk.web.view.TrackCardView;

@Service
public class TrendingService {

    private final TrackRepository trackRepository;
    private final FileStorageService fileStorageService;

    public TrendingService(TrackRepository trackRepository, FileStorageService fileStorageService) {
        this.trackRepository = trackRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public Page<TrackCardView> getTrendingTracks(Pageable pageable, String sortDirection) {
        // The pageable passed here might have sort info, but we need to ignore it for the DB query
        // because the queries have hardcoded sorting.
        // We reconstruct a Pageable with just page and size.
        Pageable dbPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());

        Page<TrackEntity> trackPage;
        if ("asc".equalsIgnoreCase(sortDirection)) {
            trackPage = trackRepository.findTrendingAsc(dbPageable);
        } else {
            trackPage = trackRepository.findTrendingDesc(dbPageable);
        }

        List<TrackCardView> views = trackPage.stream()
                .map(t -> {
                    // Calculate rank based on page offset + index + 1
                    long rank = pageable.getOffset() + trackPage.getContent().indexOf(t) + 1;

                    double avgRating = 0.0;
                    if (t.getRateCount() > 0) {
                        avgRating = (double) t.getRating() / t.getRateCount();
                    }

                    return new TrackCardView(
                            t.getId(),
                            t.getName(),
                            t.getArtist() != null ? t.getArtist().getName() : "-",
                            t.getAlbum() != null ? t.getAlbum().getName() : "-",
                            t.getAlbum() != null ? fileStorageService.toPublicUrl(t.getAlbum().getCoverUrl()) : null,
                            avgRating,
                            (int) rank
                    );
                })
                .collect(Collectors.toList());

        return new PageImpl<>(views, pageable, trackPage.getTotalElements());
    }
}
