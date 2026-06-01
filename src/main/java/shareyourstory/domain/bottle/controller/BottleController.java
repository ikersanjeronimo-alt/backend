package shareyourstory.domain.bottle.controller;

import org.springframework.web.bind.annotation.RestController;
import shareyourstory.domain.bottle.DTO.BottleDTO;
import shareyourstory.domain.bottle.model.Bottle;
import shareyourstory.domain.bottle.service.BottleService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
public class BottleController {

    @Autowired
    BottleService bottleService;

    @PostMapping("/api/bottles")
    public int createBottle(@RequestBody BottleDTO bottleDTO) {
        return bottleService.createBottle(bottleDTO);
    }

    @GetMapping("/api/bottles/received")
    public ResponseEntity<BottleDTO> receive() {
        return bottleService.getRandomBottle().map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/bottles/floating")
    public List<Bottle> getMethodName() {
        return bottleService.getFloatingBottleList();
    }

}
