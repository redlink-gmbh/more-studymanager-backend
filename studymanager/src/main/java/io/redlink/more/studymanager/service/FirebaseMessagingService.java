package io.redlink.more.studymanager.service;

import com.google.firebase.messaging.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FirebaseMessagingService {
    private final FirebaseMessaging firebaseMessaging;
    private enum apnsPriority{
        LOW(1),
        MEDIUM(5),
        HIGH(10);

        public final int value;
        apnsPriority(int value) {
            this.value = value;
        }
    }


    private static final Logger log = LoggerFactory.getLogger(FirebaseMessagingService.class);

    public FirebaseMessagingService(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    public void sendNotification(String title, String body, String token) throws FirebaseMessagingException {

        Notification notification = Notification
                .builder()
                .setTitle(title)
                .setBody(body)
                .build();

        String apnsPriorityHeader = "apns-priority";
        String apsCategory = "NEW_MESSAGE_CATEGORY";

        ApnsConfig apnsConfig = ApnsConfig
                .builder()
                .putHeader(apnsPriorityHeader, String.valueOf(apnsPriority.MEDIUM.value))
                .setAps(Aps.builder().setCategory(apsCategory).build())
                .build();

        Message message = Message
                .builder()
                .setToken(token)
                .setNotification(notification)
                .setApnsConfig(apnsConfig)
//                .putAllData()
                .build();
        if (firebaseMessaging == null) {
            log.warn("Not sending Message {}", title);
        } else {
            firebaseMessaging.send(message);
        }
    }
}
