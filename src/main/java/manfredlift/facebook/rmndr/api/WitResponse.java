package manfredlift.facebook.rmndr.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class WitResponse {
    @JsonProperty("msg_id")
    private String msgId;

    @JsonProperty("_text")
    private String text;

    private Map<String, List<NlpEntity>> entities;
}