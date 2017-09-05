package manfredlift.facebook.rmndr;

import lombok.extern.slf4j.Slf4j;
import manfredlift.facebook.rmndr.client.FbClient;
import org.quartz.*;

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

            fbClient.sendTextMessage(recipientId, text);
        } catch (SchedulerException e) {
            log.warn("Error when getting context for sending a reminder. Error: '{}:{}'",
                e.getClass().getCanonicalName(), e.getMessage());

            if (jobExecutionContext.getRefireCount() < 100) {
                throw new JobExecutionException(e, true);
            } else {
                log.error("Failed to get the job execution context after 100 retries");
            }
        }
    }
}
