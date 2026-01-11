package ru.ifmo.se.pisk.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public String trending(@PageableDefault(size = 10) Pageable pageable,
                           Model model,
                           HttpServletRequest request) {
        Page<TrackCardView> tracks = trendingService.getTrendingTracks(pageable);

        model.addAttribute("tracks", tracks);
        model.addAttribute("langLinks", LanguageLinks.build(request));

        return "trending";
    }
}
