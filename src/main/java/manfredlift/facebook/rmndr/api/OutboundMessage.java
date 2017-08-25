package manfredlift.facebook.rmndr.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@JsonInclude(NON_NULL)
@Builder
public class OutboundMessage {
    private String text;

    @JsonProperty("quick_replies")
    private List<QuickReply> quickReplies;
}
