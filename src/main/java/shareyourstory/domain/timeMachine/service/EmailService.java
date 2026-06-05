package shareyourstory.domain.timeMachine.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import shareyourstory.domain.timeMachine.model.TimeMachine;

@Service
public class EmailService {

    @Autowired
    JavaMailSender javaMailSender;

    // El From debe coincidir con la cuenta SMTP autenticada (Gmail reescribe/rechaza
    // un From distinto). Se lee de la misma propiedad que las credenciales.
    @Value("${spring.mail.username:}")
    private String fromAddress;

    public void send(TimeMachine timeMachine) throws MailException {

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(fromAddress);
        mail.setTo(timeMachine.getEmail());
        mail.setSubject("Tu mensaje programado");
        mail.setText(timeMachine.getMessage());

        javaMailSender.send(mail);
    }
}
