package shareyourstory.domain.timeMachine.controller;

import org.springframework.web.bind.annotation.RestController;
import shareyourstory.domain.timeMachine.DTO.TimeMachineDTO;
import shareyourstory.domain.timeMachine.service.TimeMachineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
public class TimeMachineController {

    @Autowired
    TimeMachineService timeMachineService;

    @PostMapping("/api/timeMachine")
    public int createTimeMachine(@RequestBody TimeMachineDTO timeMachineDTO) {
        return timeMachineService.createTimeMachine(timeMachineDTO);
    }
}
