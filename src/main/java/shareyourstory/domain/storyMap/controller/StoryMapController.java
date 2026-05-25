package shareyourstory.domain.storyMap.controller;

import org.springframework.web.bind.annotation.RestController;
import shareyourstory.domain.storyMap.model.StoryMap;
import shareyourstory.domain.storyMap.service.StoryMapService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
public class StoryMapController {

    @Autowired
    StoryMapService storyMapService;

    @GetMapping("/storyMap")
    public List<StoryMap> getAllStoryMapPoints(@RequestParam String param) {
        return storyMapService.getAllStoryMaps();
    }
}
