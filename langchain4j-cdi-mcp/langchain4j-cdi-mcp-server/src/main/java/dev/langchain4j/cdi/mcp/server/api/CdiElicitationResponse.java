package dev.langchain4j.cdi.mcp.server.api;

import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.mcp_java.server.ElicitationResponse;

/** Implementation of {@link ElicitationResponse} backed by a JSON-RPC result. */
public class CdiElicitationResponse implements ElicitationResponse {

    private final JsonObject json;

    CdiElicitationResponse(JsonObject json) {
        this.json = json;
    }

    @Override
    public Action action() {
        String actionStr = json.getString("action", "DECLINE");
        return Action.valueOf(actionStr.toUpperCase());
    }

    @Override
    public Content content() {
        JsonObject contentObj = json.containsKey("content") ? json.getJsonObject("content") : null;
        if (contentObj == null) {
            return new EmptyContent();
        }
        return new JsonContent(contentObj);
    }

    private static class EmptyContent implements Content {
        @Override
        public Boolean getBoolean(String key) {
            return null;
        }

        @Override
        public String getString(String key) {
            return null;
        }

        @Override
        public List<String> getStrings(String key) {
            return List.of();
        }

        @Override
        public Integer getInteger(String key) {
            return null;
        }

        @Override
        public Number getNumber(String key) {
            return null;
        }

        @Override
        public Map<String, Object> asMap() {
            return Map.of();
        }
    }

    private static class JsonContent implements Content {
        private final JsonObject obj;

        JsonContent(JsonObject obj) {
            this.obj = obj;
        }

        @Override
        public Boolean getBoolean(String key) {
            if (!obj.containsKey(key)) return null;
            JsonValue val = obj.get(key);
            return val.getValueType() == JsonValue.ValueType.TRUE;
        }

        @Override
        public String getString(String key) {
            if (!obj.containsKey(key)) return null;
            JsonValue val = obj.get(key);
            return val instanceof JsonString s ? s.getString() : val.toString();
        }

        @Override
        public List<String> getStrings(String key) {
            if (!obj.containsKey(key)) return List.of();
            return obj.getJsonArray(key).stream()
                    .map(v -> v instanceof JsonString s ? s.getString() : v.toString())
                    .toList();
        }

        @Override
        public Integer getInteger(String key) {
            if (!obj.containsKey(key)) return null;
            return obj.getInt(key);
        }

        @Override
        public Number getNumber(String key) {
            if (!obj.containsKey(key)) return null;
            return obj.getJsonNumber(key).numberValue();
        }

        @Override
        public Map<String, Object> asMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            obj.forEach((k, v) -> map.put(k, convertJsonValue(v)));
            return map;
        }

        private Object convertJsonValue(JsonValue val) {
            return switch (val.getValueType()) {
                case STRING -> ((JsonString) val).getString();
                case NUMBER -> obj.getJsonNumber(val.toString()).numberValue();
                case TRUE -> true;
                case FALSE -> false;
                case NULL -> null;
                default -> val.toString();
            };
        }
    }
}
