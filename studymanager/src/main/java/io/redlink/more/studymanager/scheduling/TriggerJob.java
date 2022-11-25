package io.redlink.more.studymanager.scheduling;

import io.redlink.more.studymanager.sdk.MoreSDK;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TriggerJob implements Job {

    private static Logger LOGGER = LoggerFactory.getLogger(TriggerJob.class);

    @Autowired
    MoreSDK moreSDK;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        moreSDK.testPing(context);
    }
}
