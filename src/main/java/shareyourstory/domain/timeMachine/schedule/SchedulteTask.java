package shareyourstory.domain.timeMachine.schedule;

import java.text.SimpleDateFormat;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import shareyourstory.domain.timeMachine.model.TimeMachine;
import shareyourstory.domain.timeMachine.repository.TimeMachineRepository;

@Component
public class SchedulteTask {

    @Autowired
    TimeMachineRepository timeMachineRepository;

    @Scheduled(cron = "0 0 0 * * *")
    public void checkForSendEmail() {

        List<TimeMachine> timeMachineList = timeMachineRepository.findAll();

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yy");


        for (TimeMachine timeMachine : timeMachineList) {

        }

        // READ ALL THE timeMachine dates, and if its the same day send the email.
    }

}
