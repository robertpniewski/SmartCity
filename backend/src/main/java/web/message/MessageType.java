package web.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MessageType {
    PREPARE_SIMULATION_REQUEST(1),
    PREPARE_SIMULATION_RESPONSE(2),
    START_SIMULATION_REQUEST(3),
    START_SIMULATION_RESPONSE(4),
    CREATE_CAR_INFO(5),
    UPDATE_CAR_INFO(6);

    private final int code;

    MessageType(int code) {
        this.code = code;
    }

    @JsonCreator
    public static MessageType create(int code) {
        for (var value : MessageType.values()) {
            if (value.code == code) {
                return value;
            }
        }

        throw new IllegalArgumentException("Message type with code: " + code + "was not found");
    }

    @JsonValue
    public int getCode() {
        return code;
    }
}
