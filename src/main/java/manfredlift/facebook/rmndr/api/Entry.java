package manfredlift.facebook.rmndr.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Entry {
    private long id;
    private long time;
    private List<Messaging> messaging;
}