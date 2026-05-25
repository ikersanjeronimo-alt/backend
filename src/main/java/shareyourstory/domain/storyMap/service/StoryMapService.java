package shareyourstory.domain.storyMap.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import shareyourstory.domain.storyMap.model.StoryMap;
import shareyourstory.domain.storyMap.repository.StoryMapRepository;

@Service
public class StoryMapService {

    @Autowired
    StoryMapRepository storyMapRepository;

    public List<StoryMap> getAllStoryMaps() {
        return storyMapRepository.findAll();
    }

}
