package io.redlink.more.studymanager.service;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import io.redlink.more.studymanager.model.Notification;
import io.redlink.more.studymanager.model.PushNotificationsToken;
import io.redlink.more.studymanager.repository.NotificationRepository;
import io.redlink.more.studymanager.repository.PushNotificationTokenRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PushNotificationService {
    private final PushNotificationTokenRepository pushNotificationsRepository;
    private final NotificationRepository notificationRepository;
    private final FirebaseMessagingService firebaseService;

    private static final Logger LOG = LoggerFactory.getLogger(PushNotificationService.class);

    public PushNotificationService(PushNotificationTokenRepository pushNotificationsRepository,
                                   NotificationRepository notificationRepository, FirebaseMessagingService firebaseService) {
        this.pushNotificationsRepository = pushNotificationsRepository;
        this.notificationRepository = notificationRepository;
        this.firebaseService = firebaseService;
    }

    public boolean sendPushNotification(long studyID, int participantId, String title, String message, Map<String, String> data) {
        Optional<PushNotificationsToken> tkn = this.pushNotificationsRepository.getTokenById(studyID, participantId);

        if (tkn.isEmpty()) {
            return false;
        }

        final PushNotificationsToken token = tkn.get();
        final String serviceType = token.service();
        if ("FCM".equals(serviceType)) {
            try {
                String msgId = this.firebaseService.sendNotification(title, message, data, token.token());
                if(msgId != null) {
                    LOG.info("Store Text Message (sid:{} pid:{}, mid:{})", studyID, participantId, msgId);
                    //TODO kind of workaround, data handling should be cleaned up
                    Map<String,String> dataToStore = new HashMap<>();
                    dataToStore.put("title", title);
                    dataToStore.put("body", message);
                    if(data != null && data.containsKey("deepLink")) {
                        dataToStore.put("deepLink", data.get("deepLink"));
                    }

                    this.notificationRepository.insert(
                            new Notification()
                                    .setStudyId(studyID)
                                    .setParticipantId(participantId)
                                    .setMsgId(msgId)
                                    .setType(Notification.Type.TEXT)
                                    .setData(dataToStore)
                    );
                }
                return true;
            } catch (FirebaseMessagingException e) {
                if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                    LOG.debug("Outdated FCM-Notification token (sid:{} pid:{}), trying to clean-up...", studyID, participantId);
                    if (pushNotificationsRepository.deleteToken(studyID, participantId)) {
                        LOG.info("Cleared outdated FCM-Notification token (sid:{} pid:{})", studyID, participantId);
                    }
                } else {
                    LOG.warn("Could not send Notification (sid:{} pid:{}): {}", studyID, participantId, e.getMessage(), e);
                }
                return false;
            }
        } else {
            LOG.warn("Unknown Notification-Service-Type: {} - cannot send notification (sid:{} pid:{})",
                    serviceType, studyID, participantId);
            return false;
        }
    }
}
