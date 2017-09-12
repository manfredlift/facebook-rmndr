package manfredlift.facebook.rmndr.util;

import manfredlift.facebook.rmndr.api.ReferenceTime;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class DateHelperTest {
    @Test
    public void testHelper_successPlus1() {
        ReferenceTime referenceTime = DateHelper.referenceTimeFromMillis(1505211935893L, 1);
        assertThat(referenceTime.getReferenceTime(), is(equalTo("2017-09-12T11:25:35.893+01:00")));
    }

    @Test
    public void testHelper_successMinus7() {
        ReferenceTime referenceTime = DateHelper.referenceTimeFromMillis(1505211935893L, -7);
        assertThat(referenceTime.getReferenceTime(), is(equalTo("2017-09-12T03:25:35.893-07:00")));
    }
}
