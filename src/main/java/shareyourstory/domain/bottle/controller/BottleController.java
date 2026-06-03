package shareyourstory.domain.bottle.controller;

import org.springframework.web.bind.annotation.RestController;
import shareyourstory.domain.bottle.DTO.BottleDTO;
import shareyourstory.domain.bottle.DTO.FloatingBottleResponse;
import shareyourstory.domain.bottle.service.BottleService;
import shareyourstory.domain.user.model.User;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
public class BottleController {

    @Autowired
    BottleService bottleService;

    @PostMapping("/api/bottles")
    public ResponseEntity<Void> createBottle(@RequestBody BottleDTO bottleDTO,
            @AuthenticationPrincipal User user) {
        if (bottleDTO == null || bottleDTO.message() == null || bottleDTO.message().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        bottleService.createBottle(bottleDTO, user == null ? null : user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/api/bottles/received")
    public ResponseEntity<BottleDTO> receive(@AuthenticationPrincipal User user) {
        return bottleService.receiveRandom(user == null ? null : user.getUserId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/api/bottles/floating")
    public List<FloatingBottleResponse> getFloatingBottles() {
        return bottleService.getFloatingBottleList();
    }

}
