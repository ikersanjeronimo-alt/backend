package shareyourstory.domain.timeMachine.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import shareyourstory.domain.timeMachine.model.TimeMachine;

@Service
public class EmailService {

    @Autowired
    JavaMailSender javaMailSender;

    public void send(TimeMachine timeMachine) throws MailException {

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom("shareyourstoyrypbl@gmail.com");
        mail.setTo(timeMachine.getEmail());
        mail.setSubject("Tu mensaje programado");
        mail.setText(timeMachine.getMessage());

        javaMailSender.send(mail);
    }
}
