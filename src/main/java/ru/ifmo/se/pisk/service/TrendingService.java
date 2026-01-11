package ru.ifmo.se.pisk.service;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ifmo.se.pisk.repository.TrackRepository;
import ru.ifmo.se.pisk.repository.projection.TrackSearchResult;
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
    public Page<TrackCardView> getTrendingTracks(Pageable pageable) {
        // fn_search_tracks parameters: query, genreIds, artistIds, labelIds, explicit, sort, limit, offset
        int limit = pageable.getPageSize();
        int offset = (int) pageable.getOffset();

        List<TrackSearchResult> results = trackRepository.searchTracks(
                null, // query
                null, // genreIds
                null, // artistIds
                null, // labelIds
                null, // explicit (null = all)
                "rating", // sort by rating (descending)
                limit,
                offset
        );

        // We do not have a total count from the function efficiently, but for "Trending"
        // we can assume a large number or just allow next page until empty.
        // For standard pagination UI, usually a count is needed.
        // Since fn_search_tracks doesn't return count, we might have to fetch a bit more or just live with it.
        // However, PageImpl expects a total.
        // For now, let's assume a large total for "Top" lists or maybe Count only what we have + check next?
        // Let's pass a dummy total that ensures "Next" works if we have full page,
        // but exact page counts might be off.
        // Better yet: To get total count of *all* tracks is `trackRepository.count()`.

        long total = trackRepository.count();

        List<TrackCardView> views = results.stream()
                .map(r -> {
                    // Calculate rank based on offset + index + 1
                    int rank = offset + results.indexOf(r) + 1;
                    return new TrackCardView(
                            r.getTrackId(),
                            r.getTrackName(),
                            r.getArtistName(),
                            r.getAlbumName(),
                            fileStorageService.toPublicUrl(r.getCoverUrl()),
                            r.getRatingAvg() != null ? r.getRatingAvg() : 0.0,
                            rank
                    );
                })
                .collect(Collectors.toList());

        return new PageImpl<>(views, pageable, total);
    }
}
