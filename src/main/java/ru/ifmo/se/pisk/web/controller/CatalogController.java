package ru.ifmo.se.pisk.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.ifmo.se.pisk.domain.GenreInfo;
import ru.ifmo.se.pisk.domain.TrackInfo;
import ru.ifmo.se.pisk.service.GenreService;
import ru.ifmo.se.pisk.service.TrackService;
import ru.ifmo.se.pisk.service.UserService;
import ru.ifmo.se.pisk.service.dto.TrackSearchCriteria;
import ru.ifmo.se.pisk.web.view.TrackView;
import ru.ifmo.se.pisk.web.util.LanguageLinks;

@Controller
@RequestMapping("/catalog")
public class CatalogController {

    private final TrackService trackService;
    private final GenreService genreService;
    private final UserService userService;

    public CatalogController(TrackService trackService, GenreService genreService, UserService userService) {
        this.trackService = trackService;
        this.genreService = genreService;
        this.userService = userService;
    }

    @GetMapping
    public String catalog(@RequestParam(required = false) String title,
                          @RequestParam(required = false) Boolean explicit,
                          @RequestParam(required = false) Boolean inoagent,
                          @RequestParam(name = "genreId", required = false) Set<Long> genreIds,
                          @PageableDefault(size = 9, sort = "uploadedAt", direction = Sort.Direction.DESC) Pageable pageable,
                          Model model,
                          HttpServletRequest request) {
        TrackSearchCriteria criteria = new TrackSearchCriteria(title, genreIds, null, null, explicit, inoagent);
        Page<TrackInfo> tracks = trackService.searchTracks(criteria, pageable);
        Map<Long, String> genreNames = genreService.listAll().stream()
                .collect(Collectors.toMap(GenreInfo::id, GenreInfo::title));
        Page<TrackView> view = tracks.map(info -> toView(info, genreNames));

        model.addAttribute("tracks", view);
        model.addAttribute("genres", genreService.listAll());
        model.addAttribute("titleQuery", title);
        model.addAttribute("explicitFilter", explicit);
        model.addAttribute("inoagentFilter", inoagent);
        model.addAttribute("genreFilter", genreIds);
        model.addAttribute("langLinks", LanguageLinks.build(request));
        return "catalog";
    }

    private TrackView toView(TrackInfo info, Map<Long, String> genreNames) {
        String authorName = info.authorId() != null ? userService.requireUser(info.authorId()).getUsername() : "-";
        String genreTitle = info.genreId() != null ? genreNames.getOrDefault(info.genreId(), "-") : "-";
        String coverUrl = info.coverPath() != null ? "/api/v1/tracks/" + info.id() + "/cover" : null;
        return new TrackView(
                info.id(),
                info.name(),
                info.authorId(),
                authorName,
                genreTitle,
                coverUrl,
                info.explicit(),
                info.inoagent(),
                info.averageRating(),
                info.durationSeconds(),
                info.uploadDate()
        );
    }

}
