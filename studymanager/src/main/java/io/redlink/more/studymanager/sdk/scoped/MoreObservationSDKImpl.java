package io.redlink.more.studymanager.sdk.scoped;

import io.redlink.more.studymanager.core.properties.ObservationProperties;
import io.redlink.more.studymanager.core.sdk.MoreObservationSDK;
import io.redlink.more.studymanager.sdk.MoreSDK;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public class MoreObservationSDKImpl extends MorePlatformSDKImpl implements MoreObservationSDK {

    private final int observationId;

    public MoreObservationSDKImpl(MoreSDK sdk, long studyId, Integer studyGroupId, int observationId) {
        super(sdk, studyId, studyGroupId);
        this.observationId = observationId;
    }

    @Override
    public String getIssuer() {
        return studyId + "-" + studyGroupId + '-' + observationId + "-observation";
    }

    @Override
    public int getObservationId() {
        return observationId;
    }

    @Override
    public void setPropertiesForParticipant(Integer participantId, ObservationProperties properties) {
        sdk.setPropertiesForParticipant(
                this.studyId,
                participantId,
                this.observationId,
                properties
        );
    }

    @Override
    public Optional<ObservationProperties> getPropertiesForParticipant(Integer participantId) {
        return sdk.getPropertiesForParticipant(
                this.studyId,
                participantId,
                this.observationId
        );
    }

    @Override
    public void removePropertiesForParticipant(Integer participantId) {
        sdk.removePropertiesForParticipant(
                this.studyId,
                participantId,
                this.observationId
        );
    }

    @Override
    public void storeDataPoint(Integer participantId, String observationType, Map data) {
        sdk.storeDatapoint(studyId, studyGroupId, participantId, "observation_"+observationId, observationType, Instant.now(), data);
    }
}
