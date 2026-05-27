package shareyourstory.domain.timeMachine.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.apache.catalina.connector.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import shareyourstory.domain.timeMachine.DTO.TimeMachineDTO;
import shareyourstory.domain.timeMachine.model.TimeMachine;
import shareyourstory.domain.timeMachine.repository.TimeMachineRepository;

@Service
public class TimeMachineService {

    @Autowired
    TimeMachineRepository timeMachineRepository;

    public int createTimeMachine(TimeMachineDTO newTimeMachineData) {

        TimeMachine newTimeMachine = new TimeMachine();

        newTimeMachine.setMessage(newTimeMachineData.message());
        newTimeMachine.setEmail(newTimeMachineData.deliveryDate());

        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yy");

        try {
            newTimeMachine.setDeliveryDate(format.parse(newTimeMachineData.deliveryDate()));

            timeMachineRepository.save(newTimeMachine);

            return Response.SC_CREATED;

        } catch (ParseException e) {
            System.out.println("Error while trying to parse date format.");
        } catch (Exception e) {
            System.out.println("Error while creating new Time Machine");
        }

        return Response.SC_CONFLICT;
    }
}
