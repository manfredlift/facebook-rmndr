package manfredlift.facebook.rmndr.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Postback {
    private String title;
    private String payload;
}
