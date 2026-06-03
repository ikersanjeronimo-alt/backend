package shareyourstory.domain.bottle.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import shareyourstory.domain.bottle.DTO.BottleDTO;
import shareyourstory.domain.bottle.DTO.FloatingBottleResponse;
import shareyourstory.domain.bottle.model.Bottle;
import shareyourstory.domain.bottle.repository.BottleRepository;

@Service
public class BottleService {

    @Autowired
    BottleRepository bottleRepository;

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");
    private final Random random = new Random();

    public void createBottle(BottleDTO bottleDTO, Integer authorId) {
        Bottle newBottle = new Bottle();
        newBottle.setMessage(bottleDTO.message());
        newBottle.setAuthorId(authorId);
        bottleRepository.save(newBottle);
    }

    /**
     * Entrega una botella al azar entre las NO recibidas y que no sean del propio
     * usuario; la marca como recibida para que no se reparta de nuevo. Vacio si no
     * hay ninguna disponible.
     */
    public Optional<BottleDTO> receiveRandom(Integer userId) {
        List<Bottle> candidates = bottleRepository.findByReceivedFalse().stream()
                .filter(b -> userId == null || !userId.equals(b.getAuthorId()))
                .toList();

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        Bottle chosen = candidates.get(random.nextInt(candidates.size()));
        chosen.setReceived(true);
        bottleRepository.save(chosen);
        return Optional.of(new BottleDTO(chosen.getMessage()));
    }

    public List<FloatingBottleResponse> getFloatingBottleList() {
        return bottleRepository.findTop3ByOrderByCreatedAtDesc().stream()
                .map(b -> new FloatingBottleResponse(
                        b.getMessage(),
                        b.getCreatedAt() == null ? "" : b.getCreatedAt().format(HHMM)))
                .toList();
    }
}
