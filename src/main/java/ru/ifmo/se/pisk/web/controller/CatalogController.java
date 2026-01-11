package ru.ifmo.se.pisk.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.ifmo.se.pisk.repository.projection.AlbumCardAggregate;
import ru.ifmo.se.pisk.service.GenreService;
import ru.ifmo.se.pisk.service.AlbumService;
import ru.ifmo.se.pisk.service.FileStorageService;
import ru.ifmo.se.pisk.web.view.AlbumCardView;
import ru.ifmo.se.pisk.web.util.LanguageLinks;

@Controller
@RequestMapping("/catalog")
public class CatalogController {

    private final AlbumService albumService;
    private final GenreService genreService;
    private final FileStorageService fileStorageService;

    public CatalogController(AlbumService albumService,
                             GenreService genreService,
                             FileStorageService fileStorageService) {
        this.albumService = albumService;
        this.genreService = genreService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public String catalog(@RequestParam(required = false) String title,
                          @RequestParam(required = false) Boolean explicit,
                          @RequestParam(required = false) Boolean inoagent,
                          @RequestParam(name = "genreId", required = false) Set<Long> genreIds,
                          @PageableDefault(size = 8, sort = "uploadDate", direction = Sort.Direction.DESC) Pageable pageable,
                          Model model,
                          HttpServletRequest request) {
        Page<AlbumCardAggregate> albums = albumService.searchAlbumCards(
                title,
                genreIds,
                explicit,
                inoagent,
                pageable
        );
        Page<AlbumCardView> view = albums.map(this::toView);

        model.addAttribute("albums", view);
        model.addAttribute("genres", genreService.listAll());
        model.addAttribute("titleQuery", title);
        model.addAttribute("explicitFilter", explicit);
        model.addAttribute("inoagentFilter", inoagent);
        model.addAttribute("genreFilter", genreIds);
        model.addAttribute("langLinks", LanguageLinks.build(request));
        return "catalog";
    }

    private AlbumCardView toView(AlbumCardAggregate row) {
        String coverPublicUrl = fileStorageService.toPublicUrl(row.coverUrl());
        String uploaderName = row.uploaderName() != null ? row.uploaderName() : "-";
        String artistName = row.artistName() != null ? row.artistName() : "-";
        String genreTitle = row.genreTitle() != null ? row.genreTitle() : "-";
        return new AlbumCardView(
                row.albumId(),
                row.albumName(),
                coverPublicUrl,
                row.trackNumber(),
                row.explicit(),
                row.inoagent(),
                row.averageRating(),
                row.genreId(),
                genreTitle,
                row.uploaderId(),
                uploaderName,
                row.artistId(),
                artistName,
                row.uploadDate()
        );
    }

}
