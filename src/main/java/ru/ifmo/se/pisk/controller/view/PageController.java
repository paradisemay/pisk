package ru.ifmo.se.pisk.controller.view;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.ifmo.se.pisk.controller.dto.AdminSettingsForm;
import ru.ifmo.se.pisk.controller.dto.TrackUploadForm;
import ru.ifmo.se.pisk.model.Track;
import ru.ifmo.se.pisk.service.TrackService;

import java.util.List;
import java.util.stream.Stream;

@Controller
@RequestMapping
public class PageController {

    private final TrackService trackService;

    public PageController(TrackService trackService) {
        this.trackService = trackService;
    }

    @GetMapping({"/", "/catalog"})
    public String catalog(@RequestParam(required = false) String search,
                          @RequestParam(required = false) String genre,
                          @RequestParam(required = false) List<String> tags,
                          @RequestParam(defaultValue = "rating") String sort,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "6") int size,
                          Model model) {
        List<String> normalizedTags = normalizeTags(tags);
        Page<Track> trackPage = trackService.findTracks(search, genre, normalizedTags, sort, PageRequest.of(page, size));
        model.addAttribute("trackPage", trackPage);
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentSearch", search);
        model.addAttribute("currentGenre", genre);
        model.addAttribute("currentTags", normalizedTags == null ? null : String.join(",", normalizedTags));
        model.addAttribute("genres", trackService.groupByGenre());
        return "catalog";
    }

    @GetMapping("/tracks/{id}")
    public String track(@PathVariable long id, Model model) {
        Track track = trackService.findById(id).orElseThrow(() -> new IllegalArgumentException("Трек не найден: " + id));
        model.addAttribute("track", track);
        model.addAttribute("moreFromAuthor", trackService.findByAuthor(track.getAuthor().getId()));
        return "track";
    }

    @GetMapping("/authors/{id}")
    public String author(@PathVariable long id, Model model) {
        List<Track> tracks = trackService.findByAuthor(id);
        if (tracks.isEmpty()) {
            return "redirect:/catalog";
        }
        model.addAttribute("author", tracks.getFirst().getAuthor());
        model.addAttribute("tracks", tracks);
        return "author";
    }

    @GetMapping("/upload")
    public String uploadForm(Model model) {
        model.addAttribute("trackUploadForm", new TrackUploadForm());
        model.addAttribute("genres", trackService.groupByGenre().keySet());
        return "upload";
    }

    @PostMapping("/upload")
    public String upload(@Valid @ModelAttribute TrackUploadForm trackUploadForm,
                         BindingResult bindingResult,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("genres", trackService.groupByGenre().keySet());
            return "upload";
        }
        trackService.addTrack(
                Track.builder()
                        .id(System.currentTimeMillis())
                        .title(trackUploadForm.getTitle())
                        .description(trackUploadForm.getDescription())
                        .genre(trackUploadForm.getGenre())
                        .audioUrl(trackUploadForm.getAudioUrl())
                        .coverUrl(trackUploadForm.getCoverUrl())
                        .tags(trackUploadForm.getTags())
                        .author(trackService.findById(1L).orElseThrow(() -> new IllegalArgumentException("Трек с ID 1 не найден")).getAuthor())
                        .rating(5.0)
                        .plays(0)
                        .publishedAt(java.time.LocalDate.now())
                        .duration(java.time.Duration.ofMinutes(3))
                        .build()
        );
        return "redirect:/catalog?sort=date";
    }

    @GetMapping("/admin")
    public String admin(Model model) {
        model.addAttribute("settings", new AdminSettingsForm());
        model.addAttribute("genres", trackService.groupByGenre());
        return "admin";
    }

    @PostMapping("/admin")
    public String saveAdmin(@Valid @ModelAttribute("settings") AdminSettingsForm form,
                            BindingResult bindingResult,
                            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("genres", trackService.groupByGenre());
            return "admin";
        }
        model.addAttribute("saved", true);
        model.addAttribute("genres", trackService.groupByGenre());
        return "admin";
    }

    private List<String> normalizeTags(List<String> raw) {
        if (raw == null) {
            return null;
        }
        return raw.stream()
                .flatMap(tag -> Stream.of(tag.split(",")))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }
}
