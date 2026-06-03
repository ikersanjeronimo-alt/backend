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

    /**
     * Cada medianoche entrega TODAS las cartas cuya fecha de entrega ya llego
     * (deliveryDate <= ahora), no solo las del dia exacto: asi un dia caido o un
     * fallo SMTP no pierde la carta. Solo se borra tras enviarse con exito; si el
     * envio falla, se reintenta en el siguiente ciclo.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void checkForSendEmail() {
        Date now = new Date();

        for (TimeMachine timeMachine : timeMachineRepository.findAll()) {
            if (timeMachine.getDeliveryDate() != null && !timeMachine.getDeliveryDate().after(now)) {
                try {
                    emailService.send(timeMachine);
                    timeMachineRepository.delete(timeMachine);
                } catch (Exception e) {
                    System.out.println("Error enviando carta " + timeMachine.getId() + ": " + e.getMessage());
                }
            }
        }
    }
}
