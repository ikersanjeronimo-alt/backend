package shareyourstory.domain.timeMachine.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import shareyourstory.domain.timeMachine.DTO.TimeMachineDTO;
import shareyourstory.domain.timeMachine.model.TimeMachine;
import shareyourstory.domain.timeMachine.repository.TimeMachineRepository;

@Service
public class TimeMachineService {

    @Autowired
    TimeMachineRepository timeMachineRepository;

    /**
     * Crea una carta programada. La fecha de entrega la elige el usuario (formato
     * dd-MM-yyyy). Devuelve false si faltan datos o la fecha no es valida.
     */
    public boolean createTimeMachine(TimeMachineDTO data) {
        if (data == null
                || data.message() == null || data.message().isBlank()
                || data.email() == null || data.email().isBlank()
                || data.deliveryDate() == null || data.deliveryDate().isBlank()) {
            return false;
        }

        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
        format.setLenient(false);

        try {
            TimeMachine newTimeMachine = new TimeMachine();
            newTimeMachine.setMessage(data.message());
            newTimeMachine.setEmail(data.email());
            newTimeMachine.setDeliveryDate(format.parse(data.deliveryDate()));
            timeMachineRepository.save(newTimeMachine);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }
}
