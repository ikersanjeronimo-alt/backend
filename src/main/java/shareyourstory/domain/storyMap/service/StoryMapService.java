package shareyourstory.domain.storyMap.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shareyourstory.domain.moderation.model.Report;
import shareyourstory.domain.moderation.repository.ReportRepository;
import shareyourstory.domain.storyMap.dto.NewStoryMapRequest;
import shareyourstory.domain.storyMap.model.StoryMap;
import shareyourstory.domain.storyMap.repository.StoryMapRepository;
import shareyourstory.websocket.service.WebSocketService;

@Service
public class StoryMapService {

    @Autowired
    StoryMapRepository storyMapRepository;

    @Autowired
    ReportRepository reportRepository;

    @Autowired
    WebSocketService webSocketService;

    public List<StoryMap> getAllStoryMaps() {
        return storyMapRepository.findAll();
    }

    public StoryMap createStoryMap(NewStoryMapRequest newStoryMapRequest) {
        StoryMap newStoryMap = new StoryMap();
        newStoryMap.setMessage(newStoryMapRequest.text());
        newStoryMap.setLatitude(newStoryMapRequest.lat());
        newStoryMap.setLongitude(newStoryMapRequest.lng());

        StoryMap saved = storyMapRepository.save(newStoryMap);
        webSocketService.broadcastNewStoryMap(saved);
        return saved;
    }

    /**
     * Borra una historia del mapa (acción de moderación directa). Antes desliga
     * los reportes que la referencian (FK story_id) para no violar la restricción,
     * igual que hace la resolución de un reporte STORY. Difunde el borrado por WS.
     */
    @Transactional
    public boolean deleteStory(Integer id) {
        StoryMap story = storyMapRepository.findById(id).orElse(null);
        if (story == null) {
            return false;
        }
        List<Report> referencing = reportRepository.findByStory_Id(id);
        referencing.forEach(r -> r.setStory(null));
        reportRepository.saveAll(referencing);
        reportRepository.flush();

        storyMapRepository.delete(story);
        storyMapRepository.flush();

        webSocketService.broadcastDeletedStoryMap(id);
        return true;
    }
}
