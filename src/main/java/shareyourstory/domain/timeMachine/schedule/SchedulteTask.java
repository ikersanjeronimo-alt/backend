package shareyourstory.domain.timeMachine.schedule;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import shareyourstory.domain.timeMachine.model.TimeMachine;
import shareyourstory.domain.timeMachine.repository.TimeMachineRepository;
import shareyourstory.domain.timeMachine.service.EmailService;

@Component
public class SchedulteTask {


    @Autowired
    TimeMachineRepository timeMachineRepository;

    @Autowired
    EmailService emailService;

    @Scheduled(cron = "0 0 0 * * *")
    public void checkForSendEmail() {

        List<TimeMachine> timeMachineList = timeMachineRepository.findAll();

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        String todayDate = dateFormat.format(new Date());

        for (TimeMachine timeMachine : timeMachineList) {
            if (timeMachine.getDeliveryDate() != null) {
                String deliveryDateStr = dateFormat.format(timeMachine.getDeliveryDate());

                if (todayDate.equals(deliveryDateStr)) {
                    emailService.send(timeMachine);
                    timeMachineRepository.delete(timeMachine);
                }
            }
        }
    }
}
