package manfredlift.facebook.rmndr.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Messaging {
    private User sender;
    private User recipient;
    private long timestamp;
    private Message message;
    private Postback postback;
}