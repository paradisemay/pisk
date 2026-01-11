package ru.ifmo.se.pisk.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.ifmo.se.pisk.service.TrendingService;
import ru.ifmo.se.pisk.web.util.LanguageLinks;
import ru.ifmo.se.pisk.web.view.TrackCardView;

@Controller
@RequestMapping("/trending")
public class TrendingController {

    private final TrendingService trendingService;

    public TrendingController(TrendingService trendingService) {
        this.trendingService = trendingService;
    }

    @GetMapping
    public String trending(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size,
                           @RequestParam(required = false, defaultValue = "desc") String sort,
                           Model model,
                           HttpServletRequest request) {

        Pageable pageable = PageRequest.of(page, size);

        Page<TrackCardView> tracks = trendingService.getTrendingTracks(pageable, sort);

        model.addAttribute("tracks", tracks);
        model.addAttribute("langLinks", LanguageLinks.build(request));
        model.addAttribute("currentSort", sort);

        return "trending";
    }
}
