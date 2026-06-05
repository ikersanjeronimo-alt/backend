package shareyourstory.domain.timeMachine.schedule;

import java.util.Date;
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

    // Cada medianoche entrega las cartas con deliveryDate <= ahora. Si el envio falla,
    // no se borra la carta y se reintenta en el siguiente ciclo.
    @Scheduled(cron = "0 0 0 * * *")
    public void checkForSendEmail() {
        Date now = new Date();

        for (TimeMachine timeMachine : timeMachineRepository.findAll()) {
            if (timeMachine.getDeliveryDate() != null && !timeMachine.getDeliveryDate().after(now)) {
                try {
                    emailService.send(timeMachine);
                    timeMachineRepository.delete(timeMachine);
                } catch (Exception ignored) {
                }
            }
        }
    }
}
