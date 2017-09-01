package manfredlift.facebook.rmndr.api;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReferenceTime {
    @SerializedName("reference_time")
    private String referenceTime;
}
