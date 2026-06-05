package shareyourstory.domain.timeMachine.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import shareyourstory.domain.timeMachine.DTO.TimeMachineDTO;
import shareyourstory.domain.timeMachine.model.TimeMachine;
import shareyourstory.domain.timeMachine.repository.TimeMachineRepository;

@Service
public class TimeMachineService {

    @Autowired
    TimeMachineRepository timeMachineRepository;

    public void createTimeMachine(TimeMachineDTO data) {
        if (data == null
                || data.message() == null || data.message().isBlank()
                || data.email() == null || data.email().isBlank()
                || data.deliveryDate() == null || data.deliveryDate().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faltan campos obligatorios");
        }

        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
        format.setLenient(false);

        try {
            TimeMachine newTimeMachine = new TimeMachine();
            newTimeMachine.setMessage(data.message());
            newTimeMachine.setEmail(data.email());
            newTimeMachine.setDeliveryDate(format.parse(data.deliveryDate()));
            timeMachineRepository.save(newTimeMachine);
        } catch (ParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de fecha invalido (dd-MM-yyyy)");
        }
    }
}
