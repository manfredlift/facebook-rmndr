package manfredlift.facebook.rmndr;

import lombok.extern.slf4j.Slf4j;
import manfredlift.facebook.rmndr.client.FbClient;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerContext;

@Slf4j
public class  ReminderJob implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String recipientId;
        String text;
        FbClient fbClient;

        try {
            recipientId = (String) jobExecutionContext.getJobDetail().getJobDataMap().get("recipient");
            text = (String) jobExecutionContext.getJobDetail().getJobDataMap().get("text");

            SchedulerContext context = jobExecutionContext.getScheduler().getContext();
            fbClient = (FbClient) context.get(RmndrConstants.FB_CLIENT);
        } catch (Exception e) {
            log.error("Error when getting context for sending a reminder '{}:{}' when sending a message",
                e.getClass().getCanonicalName(), e.getMessage());
            return;
        }

        fbClient.sendTextMessage(recipientId, text);
    }
}
