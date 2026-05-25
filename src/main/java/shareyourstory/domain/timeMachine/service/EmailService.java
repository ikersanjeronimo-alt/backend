package shareyourstory.domain.timeMachine.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService implements MailSender {

    @Autowired
    JavaMailSender javaMailSender;

    @Override
    public void send(SimpleMailMessage... simpleMessages) throws MailException {

        SimpleMailMessage mail = new SimpleMailMessage();

        mail.setFrom("ikersanjeronimo@gmail.com");
        mail.setTo("iker.sanjeronimo@alumni.mondragon.edu");
        mail.setText("TEST EMAIL");
        javaMailSender.send(mail);
    }
}
