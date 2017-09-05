package manfredlift.facebook.rmndr;

import manfredlift.facebook.rmndr.client.FbClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ReminderJobTest {
    private final static String RECIPIENT = "some_recipient";
    private final static String TEXT = "some_text";

    private ReminderJob reminderJob;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private JobExecutionContext jobExecutionContext;

    @Mock private FbClient fbClient;

    @Before
    public void setup() throws SchedulerException {
        reminderJob = new ReminderJob();
        when(jobExecutionContext.getScheduler().getContext().get(RmndrConstants.FB_CLIENT)).thenReturn(fbClient);

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("recipient", RECIPIENT);
        jobDataMap.put("text", TEXT);

        when(jobExecutionContext.getJobDetail().getJobDataMap()).thenReturn(jobDataMap);
    }

    @Test
    public void executionTest_success() throws JobExecutionException {
        reminderJob.execute(jobExecutionContext);

        verify(fbClient).sendTextMessage(RECIPIENT, TEXT);
        verifyNoMoreInteractions(fbClient);
    }

    @Test(expected=JobExecutionException.class)
    public void executionTest_exception() throws Exception {
        when(jobExecutionContext.getScheduler().getContext()).thenThrow(SchedulerException.class);

        reminderJob.execute(jobExecutionContext);
        verifyZeroInteractions(fbClient);
    }

    @Test
    public void executionTest_exceptionAfter100Retries() throws Exception {
        when(jobExecutionContext.getScheduler().getContext()).thenThrow(SchedulerException.class);
        when(jobExecutionContext.getRefireCount()).thenReturn(1000);

        reminderJob.execute(jobExecutionContext);
        verifyZeroInteractions(fbClient);
    }
}
