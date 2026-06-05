package shareyourstory.domain.timeMachine.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import shareyourstory.domain.timeMachine.DTO.TimeMachineDTO;
import shareyourstory.domain.timeMachine.service.TimeMachineService;

@RestController
public class TimeMachineController {

    @Autowired
    TimeMachineService timeMachineService;

    @PostMapping("/api/timeMachine")
    public ResponseEntity<Void> createTimeMachine(@RequestBody TimeMachineDTO timeMachineDTO) {
        timeMachineService.createTimeMachine(timeMachineDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
