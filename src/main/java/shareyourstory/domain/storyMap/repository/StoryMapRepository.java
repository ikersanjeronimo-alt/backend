package shareyourstory.domain.storyMap.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import shareyourstory.domain.storyMap.model.StoryMap;

@Repository
public interface StoryMapRepository extends JpaRepository<StoryMap, Integer> {
}
