package manfredlift.facebook.rmndr.api;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class ReferenceTime {
    @SerializedName("reference_time")
    private String referenceTime;
}
