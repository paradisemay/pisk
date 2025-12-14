package ru.ifmo.se.pisk.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.ifmo.se.pisk.domain.GenreInfo;
import ru.ifmo.se.pisk.domain.TrackInfo;
import ru.ifmo.se.pisk.service.GenreService;
import ru.ifmo.se.pisk.service.TrackService;
import ru.ifmo.se.pisk.service.UserService;
import ru.ifmo.se.pisk.web.view.TrackView;

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
                          HttpServletRequest request,
                          Model model) {
        Page<TrackInfo> tracks = trackService.searchTracks(explicit, inoagent, title, genreIds, pageable);  // TODO: fix
        Map<Long, String> genreNames = genreService.listAll().stream()
                .collect(Collectors.toMap(GenreInfo::id, GenreInfo::title));
        Page<TrackView> view = tracks.map(info -> toView(info, genreNames));

        model.addAttribute("tracks", view);
        model.addAttribute("genres", genreService.listAll());
        model.addAttribute("titleQuery", title);
        model.addAttribute("explicitFilter", explicit);
        model.addAttribute("inoagentFilter", inoagent);
        model.addAttribute("genreFilter", genreIds);
        model.addAttribute("langLinks", languageLinks(request));
        return "catalog";
    }

    private TrackView toView(TrackInfo info, Map<Long, String> genreNames) {
        String authorName = info.authorId() != null ? userService.requireUser(info.authorId()).getUsername() : "-";
        String genreTitle = info.genreId() != null ? genreNames.getOrDefault(info.genreId(), "-") : "-";
        return new TrackView(
                info.id(),
                info.name(),
                info.authorId(),
                authorName,
                genreTitle,
                info.explicit(),
                info.inoagent(),
                info.averageRating(),
                info.durationSeconds(),
                info.uploadDate()
        );
    }

    private Map<String, String> languageLinks(HttpServletRequest request) {
        String base = ServletUriComponentsBuilder.fromRequest(request).replaceQueryParam("lang").toUriString();
        Map<String, String> links = new HashMap<>();
        links.put("ru", ServletUriComponentsBuilder.fromUriString(base).replaceQueryParam("lang", "ru").toUriString());
        links.put("en", ServletUriComponentsBuilder.fromUriString(base).replaceQueryParam("lang", "en").toUriString());
        return links;
    }
}
