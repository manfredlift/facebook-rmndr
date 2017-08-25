package manfredlift.facebook.rmndr.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {
    private String mid;
    private long seq;
    private String text;
    private Nlp nlp;

    @JsonProperty("quick_reply")
    private QuickReply quickReply;
}