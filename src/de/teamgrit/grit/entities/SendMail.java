/*
 * Copyright (C) 2015 VARCID
 * Copyright (C) 2014 Team GRIT
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.teamgrit.grit.entities;

import java.util.Properties;
import java.util.logging.Logger;

import javax.swing.*;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * The Class sends an email. It is called by the class
 * {@link Exercise}.
 *
 * @author <a href="mailto:cedric.sehrer@uni-konstanz.de">Cedric Sehrer</a>
 */
public class SendMail {

    private static final Logger LOGGER = Logger.getLogger("systemlog");

    private String userName;
    private String password;
    private String sendingHost;
    private int sendingPort;
    private String from;
    private String to;
    private String subject;
    private String text;

    public void setAccountDetails(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    public void sendEMail(String from, String to, String subject, String text) {
        this.from = from;
        this.to = to;
        this.subject = subject;
        this.text = text;

        //If a GMAIL account is used to send the emails the host and port should be as follows:
        this.sendingHost = Controller.getController().getConfig().getSmtpHost(); //Bsp.: "smtp.gmail.com";
        this.sendingPort = 465;

        Properties props = new Properties();

        props.put("mail.smtp.host", this.sendingHost);
        props.put("mail.smtp.port", String.valueOf(this.sendingPort));
        props.put("mail.smtp.user", this.userName);
        props.put("mail.smtp.password", this.password);
        props.put("mail.smtp.auth", "true");

        Session session = Session.getDefaultInstance(props);

        Message message = new MimeMessage(session);

        try {
            InternetAddress fromAddress = new InternetAddress(this.from);
            InternetAddress toAddress = new InternetAddress(this.to);

            message.setFrom(fromAddress);
            message.setRecipient(Message.RecipientType.TO, toAddress);
            //to add CC or BCC use:
            //message.setRecipient(RecipientType.CC, new InternetAddress("CC_Recipient@someMail.com"));
            //message.setRecipient(RecipientType.BCC, new InternetAddress("BCC_Recipient@someMail.com"));

            message.setSubject(this.subject);
            message.setText(this.text);

            //The following is needed for GMAIL:
            Transport transport = session.getTransport("smtps");
            transport.connect(this.sendingHost,sendingPort, this.userName, this.password);
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
            //If you are not using GMAIL it could also be: Transport.send(simpleMessage);
        } catch (AddressException e) {
            LOGGER.severe("AddressException during SendMail " + e.getMessage());
        } catch (MessagingException e) {
            LOGGER.severe("MessagingException during SendMail " + e.getMessage());
        }
    }
}