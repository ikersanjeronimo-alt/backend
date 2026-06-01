package shareyourstory.domain.bottle.service;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.apache.catalina.connector.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import shareyourstory.domain.bottle.DTO.BottleDTO;
import shareyourstory.domain.bottle.model.Bottle;
import shareyourstory.domain.bottle.repository.BottleRepository;

@Service
public class BottleService {

    @Autowired
    BottleRepository bottleRepository;

    public int createBottle(BottleDTO bottleDTO) {
        Bottle newBottle = new Bottle();
        newBottle.setMessage(bottleDTO.message());

        try {
            bottleRepository.save(newBottle);
            return Response.SC_CREATED;
        } catch (Exception e) {
            return Response.SC_CONFLICT;
        }
    }

    public Optional<BottleDTO> getRandomBottle() {
        List<Bottle> bottlesList = bottleRepository.findAll();

        if (bottlesList == null) {
            return null;
        } else {
            Random rm = new Random();
            Bottle randomBottle;
            randomBottle = bottlesList.get(rm.nextInt(bottlesList.size()));

            return Optional.ofNullable(new BottleDTO(randomBottle.getMessage()));
        }
    }

    public List<Bottle> getFloatingBottleList() {
        List<Bottle> bottleList = bottleRepository.findAll();

        if (bottleList.size() > 3) {
            return bottleList.subList(0, 3);
        }

        return bottleList;
    }
}
