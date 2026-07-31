package dev.langchain4j.cdi.mcp.server.protocol;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import java.util.Base64;
import java.util.Optional;
import org.mcpjava.server.completion.CompletionResult;
import org.mcpjava.server.content.Annotations;
import org.mcpjava.server.content.AudioContent;
import org.mcpjava.server.content.ContentBlock;
import org.mcpjava.server.content.EmbeddedResource;
import org.mcpjava.server.content.ImageContent;
import org.mcpjava.server.content.ResourceLink;
import org.mcpjava.server.content.TextContent;
import org.mcpjava.server.prompts.PromptMessage;
import org.mcpjava.server.prompts.PromptResponse;
import org.mcpjava.server.resources.BlobResourceContents;
import org.mcpjava.server.resources.ResourceContents;
import org.mcpjava.server.resources.TextResourceContents;
import org.mcpjava.server.tools.ToolResponse;

/** Serializes org.mcpjava SPI-backed types to JSON-P for MCP wire-format responses. */
public final class McpJsonSerializer {

    private McpJsonSerializer() {}

    // --- Content blocks ---

    public static JsonObjectBuilder contentBlockToJson(ContentBlock block) {
        JsonObjectBuilder json = Json.createObjectBuilder();
        if (block instanceof TextContent tc) {
            json.add("type", "text");
            json.add("text", tc.text());
            addAnnotations(json, tc.annotations());
        } else if (block instanceof ImageContent ic) {
            json.add("type", "image");
            json.add("data", Base64.getEncoder().encodeToString(ic.data()));
            json.add("mimeType", ic.mimeType());
            addAnnotations(json, ic.annotations());
        } else if (block instanceof AudioContent ac) {
            json.add("type", "audio");
            json.add("data", Base64.getEncoder().encodeToString(ac.data()));
            json.add("mimeType", ac.mimeType());
            addAnnotations(json, ac.annotations());
        } else if (block instanceof EmbeddedResource er) {
            json.add("type", "resource");
            JsonObjectBuilder resource = Json.createObjectBuilder();
            resource.add("uri", er.resource().uri());
            er.resource().mimeType().ifPresent(m -> resource.add("mimeType", m));
            if (er.resource() instanceof TextResourceContents trc) {
                resource.add("text", trc.text());
            } else if (er.resource() instanceof BlobResourceContents brc) {
                resource.add("blob", Base64.getEncoder().encodeToString(brc.blob()));
            }
            json.add("resource", resource);
            addAnnotations(json, er.annotations());
        } else if (block instanceof ResourceLink rl) {
            json.add("type", "resource_link");
            json.add("name", rl.name());
            json.add("uri", rl.uri());
            if (!rl.title().equals(rl.name())) {
                json.add("title", rl.title());
            }
            rl.description().ifPresent(d -> json.add("description", d));
            rl.mimeType().ifPresent(m -> json.add("mimeType", m));
            if (rl.size().isPresent()) {
                json.add("size", rl.size().getAsLong());
            }
            addAnnotations(json, rl.annotations());
        }
        return json;
    }

    // --- Tool responses ---

    public static JsonObject toolResponseToJson(ToolResponse response) {
        JsonObjectBuilder json = Json.createObjectBuilder();
        JsonArrayBuilder contentArray = Json.createArrayBuilder();
        for (ContentBlock block : response.content()) {
            contentArray.add(contentBlockToJson(block));
        }
        json.add("content", contentArray);
        if (response.isError()) {
            json.add("isError", true);
        }
        response.structuredContent().ifPresent(sc -> json.add("structuredContent", sc.toString()));
        return json.build();
    }

    public static JsonObject plainTextToolResult(Object callResult) {
        JsonObjectBuilder rb = Json.createObjectBuilder();
        JsonArrayBuilder contentArray = Json.createArrayBuilder();
        if (callResult != null) {
            contentArray.add(Json.createObjectBuilder().add("type", "text").add("text", callResult.toString()));
        }
        rb.add("content", contentArray);
        return rb.build();
    }

    // --- Prompt responses ---

    public static JsonObject promptResponseToJson(PromptResponse response) {
        JsonObjectBuilder json = Json.createObjectBuilder();
        response.description().ifPresent(d -> json.add("description", d));
        JsonArrayBuilder messages = Json.createArrayBuilder();
        for (PromptMessage msg : response.messages()) {
            messages.add(promptMessageToJson(msg));
        }
        json.add("messages", messages);
        return json.build();
    }

    public static JsonObjectBuilder promptMessageToJson(PromptMessage message) {
        JsonObjectBuilder json = Json.createObjectBuilder();
        json.add("role", message.role().name().toLowerCase());
        json.add("content", contentBlockToJson(message.content()));
        return json;
    }

    public static JsonObjectBuilder plainTextPromptMessage(String text) {
        return Json.createObjectBuilder()
                .add("role", "user")
                .add("content", Json.createObjectBuilder().add("type", "text").add("text", text));
    }

    public static JsonObjectBuilder contentBlockMessageToJson(String role, ContentBlock content) {
        JsonObjectBuilder json = Json.createObjectBuilder();
        json.add("role", role);
        json.add("content", contentBlockToJson(content));
        return json;
    }

    // --- Resource contents ---

    public static JsonObjectBuilder resourceContentsToJson(ResourceContents contents) {
        JsonObjectBuilder json = Json.createObjectBuilder();
        json.add("uri", contents.uri());
        contents.mimeType().ifPresent(m -> json.add("mimeType", m));
        if (contents instanceof TextResourceContents trc) {
            json.add("text", trc.text());
        } else if (contents instanceof BlobResourceContents brc) {
            json.add("blob", Base64.getEncoder().encodeToString(brc.blob()));
        }
        return json;
    }

    public static JsonObjectBuilder plainTextResourceContents(String uri, String text, String mimeType) {
        JsonObjectBuilder rc = Json.createObjectBuilder().add("uri", uri).add("text", text);
        if (mimeType != null) {
            rc.add("mimeType", mimeType);
        }
        return rc;
    }

    // --- Completion ---

    public static JsonObject completionResultToJson(CompletionResult result) {
        JsonObjectBuilder json = Json.createObjectBuilder();
        JsonObjectBuilder completion = Json.createObjectBuilder();
        JsonArrayBuilder values = Json.createArrayBuilder();
        result.values().forEach(values::add);
        completion.add("values", values);
        if (result.total().isPresent()) {
            completion.add("total", result.total().getAsInt());
        }
        result.hasMore().ifPresent(hm -> completion.add("hasMore", hm));
        json.add("completion", completion);
        return json.build();
    }

    private static void addAnnotations(JsonObjectBuilder json, Optional<Annotations> annotations) {
        if (annotations == null) {
            return;
        }
        annotations.ifPresent(ann -> {
            JsonObjectBuilder annJson = Json.createObjectBuilder();
            boolean hasContent = false;
            if (ann.audience().isPresent()) {
                JsonArrayBuilder audience = Json.createArrayBuilder();
                ann.audience().get().forEach(role -> audience.add(role.name().toLowerCase()));
                annJson.add("audience", audience);
                hasContent = true;
            }
            if (ann.priority().isPresent()) {
                annJson.add("priority", ann.priority().getAsDouble());
                hasContent = true;
            }
            if (ann.lastModified().isPresent()) {
                annJson.add("lastModified", ann.lastModified().get().toString());
                hasContent = true;
            }
            if (hasContent) {
                json.add("annotations", annJson);
            }
        });
    }
}
