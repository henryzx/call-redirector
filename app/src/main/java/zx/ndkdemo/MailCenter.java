package zx.ndkdemo;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import android.util.Base64;

/**
 * Created by zhengxiao on 11/2/16.
 */

public class MailCenter extends javax.mail.Authenticator {

    private final String host;
    private final String account;
    private final String password;
    private Session session;

    public MailCenter(String host, String account, String password) {
        this.host = host;
        this.account = account;
        this.password = new String(Base64.decode(password, Base64.NO_WRAP));
        Properties props = new Properties();

        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class",
                "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.fallback", "false");
        props.setProperty("mail.smtp.quitwait", "false");

        session = Session.getDefaultInstance(props, this);

    }

    public void sendMail(String subject, String body, String from, String recipients) throws MessagingException {
        Transport transport = null;
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setSubject(subject);
            message.setContent(body, "text/plain;charset=UTF-8");
            if (recipients.indexOf(',') > 0) {
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients));
            } else {
                message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipients));
            }
            transport = session.getTransport("smtp");
            transport.connect(host, account, password);
            transport.sendMessage(message, message.getAllRecipients());
        } catch (MessagingException e) {
            throw e;
        } finally {
            if (transport != null) {
                try {
                    transport.close();
                } catch (MessagingException e) {
                    // ignored
                }
            }
        }
    }

}
