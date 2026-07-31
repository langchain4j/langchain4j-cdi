package dev.langchain4j.cdi.mcp.server.spi;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import org.mcpjava.server.Icon;
import org.mcpjava.server.MetaCarrier;
import org.mcpjava.server.Role;
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
import org.mcpjava.server.resources.ResourceResponse;
import org.mcpjava.server.resources.TextResourceContents;
import org.mcpjava.server.spi.McpServerSPI;
import org.mcpjava.server.tools.ToolResponse;

/**
 * {@link McpServerSPI} implementation providing immutable runtime types for the {@code org.mcpjava} SPI contract.
 *
 * <p>Each factory method returns a builder whose {@code build()} produces a defensively-copied, immutable instance.
 * Collections are frozen via {@code Map.copyOf} / {@code List.copyOf} to guarantee thread-safety. Registered via
 * {@code META-INF/services/org.mcpjava.server.spi.McpServerSPI}.
 */
public class CdiMcpServerSPI implements McpServerSPI {

    // ---- base builders ----

    @SuppressWarnings("unchecked")
    abstract static class MetaBuilder<B extends MetaCarrier.Builder<B>> implements MetaCarrier.Builder<B> {

        final Map<String, Object> metadata = new HashMap<>();

        @Override
        public B putMetadata(String key, Object value) {
            metadata.put(key, value);
            return (B) this;
        }

        @Override
        public B setMetadata(Map<String, Object> m) {
            metadata.clear();
            metadata.putAll(m);
            return (B) this;
        }
    }

    @SuppressWarnings("unchecked")
    abstract static class AnnotatedMetaBuilder<B extends MetaCarrier.Builder<B>> extends MetaBuilder<B> {

        Annotations annotations;

        B setAnnotations(Annotations a) {
            this.annotations = a;
            return (B) this;
        }

        Optional<Annotations> optAnnotations() {
            return Optional.ofNullable(annotations);
        }
    }

    // ---- records ----

    private record TextContentImpl(String text, Optional<Annotations> annotations, Map<String, Object> metadata)
            implements TextContent {}

    private record AudioContentImpl(
            byte[] data, String mimeType, Optional<Annotations> annotations, Map<String, Object> metadata)
            implements AudioContent {
        @Override
        public byte[] data() {
            return data != null ? data.clone() : null;
        }
    }

    private record ImageContentImpl(
            byte[] data, String mimeType, Optional<Annotations> annotations, Map<String, Object> metadata)
            implements ImageContent {
        @Override
        public byte[] data() {
            return data != null ? data.clone() : null;
        }
    }

    private record EmbeddedResourceImpl(
            ResourceContents resource, Optional<Annotations> annotations, Map<String, Object> metadata)
            implements EmbeddedResource {}

    private record ResourceLinkImpl(
            String name,
            String title,
            String uri,
            Optional<String> description,
            Optional<String> mimeType,
            Optional<Annotations> annotations,
            OptionalLong size,
            Map<String, Object> metadata)
            implements ResourceLink {}

    private record AnnotationsImpl(
            Optional<Set<Role>> audience, OptionalDouble priority, Optional<Instant> lastModified)
            implements Annotations {}

    private record PromptMessageImpl(Role role, ContentBlock content) implements PromptMessage {}

    private record PromptResponseImpl(
            Optional<String> description, List<PromptMessage> messages, Map<String, Object> metadata)
            implements PromptResponse {}

    private record TextResourceContentsImpl(
            String uri, String text, Optional<String> mimeType, Map<String, Object> metadata)
            implements TextResourceContents {}

    private record BlobResourceContentsImpl(
            String uri, byte[] blob, Optional<String> mimeType, Map<String, Object> metadata)
            implements BlobResourceContents {
        @Override
        public byte[] blob() {
            return blob != null ? blob.clone() : null;
        }
    }

    private record ResourceResponseImpl(List<ResourceContents> contents, Map<String, Object> metadata)
            implements ResourceResponse {
        @Override
        public List<ResourceContents> getContents() {
            return contents;
        }
    }

    private record CompletionResultImpl(
            List<String> values, OptionalInt total, Optional<Boolean> hasMore, Map<String, Object> metadata)
            implements CompletionResult {}

    private record ToolResponseImpl(
            List<ContentBlock> content,
            Optional<Object> structuredContent,
            boolean isError,
            Map<String, Object> metadata)
            implements ToolResponse {}

    private record IconImpl(String src, Optional<String> mimeType, List<String> sizes, Optional<Icon.Theme> theme)
            implements Icon {}

    // ---- builder implementations ----

    private static final class TextContentBuilderImpl extends AnnotatedMetaBuilder<TextContent.Builder>
            implements TextContent.Builder {

        private final String text;

        TextContentBuilderImpl(String text) {
            this.text = java.util.Objects.requireNonNull(text, "text");
        }

        @Override
        public TextContent.Builder setAnnotations(Annotations a) {
            return super.setAnnotations(a);
        }

        @Override
        public TextContent build() {
            return new TextContentImpl(text, optAnnotations(), Map.copyOf(metadata));
        }
    }

    private static final class AudioContentBuilderImpl extends AnnotatedMetaBuilder<AudioContent.Builder>
            implements AudioContent.Builder {

        private final byte[] data;
        private final String mimeType;

        AudioContentBuilderImpl(byte[] data, String mimeType) {
            this.data = data != null ? data.clone() : null;
            this.mimeType = mimeType;
        }

        @Override
        public AudioContent.Builder setAnnotations(Annotations a) {
            return super.setAnnotations(a);
        }

        @Override
        public AudioContent build() {
            return new AudioContentImpl(data, mimeType, optAnnotations(), Map.copyOf(metadata));
        }
    }

    private static final class ImageContentBuilderImpl extends AnnotatedMetaBuilder<ImageContent.Builder>
            implements ImageContent.Builder {

        private final byte[] data;
        private final String mimeType;

        ImageContentBuilderImpl(byte[] data, String mimeType) {
            this.data = data != null ? data.clone() : null;
            this.mimeType = mimeType;
        }

        @Override
        public ImageContent.Builder setAnnotations(Annotations a) {
            return super.setAnnotations(a);
        }

        @Override
        public ImageContent build() {
            return new ImageContentImpl(data, mimeType, optAnnotations(), Map.copyOf(metadata));
        }
    }

    private static final class EmbeddedResourceBuilderImpl extends AnnotatedMetaBuilder<EmbeddedResource.Builder>
            implements EmbeddedResource.Builder {

        private final String uri;
        private final String text;
        private final byte[] data;
        private String mimeType;
        private final Map<String, Object> resourceMeta = new HashMap<>();

        EmbeddedResourceBuilderImpl(String text, String uri) {
            this.text = text;
            this.data = null;
            this.uri = uri;
        }

        EmbeddedResourceBuilderImpl(byte[] data, String uri) {
            this.text = null;
            this.data = data != null ? data.clone() : null;
            this.uri = uri;
        }

        @Override
        public EmbeddedResource.Builder setAnnotations(Annotations a) {
            return super.setAnnotations(a);
        }

        @Override
        public EmbeddedResource.Builder setMimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        @Override
        public EmbeddedResource.Builder putResourceMeta(String key, Object value) {
            resourceMeta.put(key, value);
            return this;
        }

        @Override
        public EmbeddedResource build() {
            ResourceContents resource;
            if (text != null) {
                resource = new TextResourceContentsImpl(
                        uri, text, Optional.ofNullable(mimeType), Map.copyOf(resourceMeta));
            } else {
                resource = new BlobResourceContentsImpl(
                        uri, data, Optional.ofNullable(mimeType), Map.copyOf(resourceMeta));
            }
            return new EmbeddedResourceImpl(resource, optAnnotations(), Map.copyOf(metadata));
        }
    }

    private static final class ResourceLinkBuilderImpl extends AnnotatedMetaBuilder<ResourceLink.Builder>
            implements ResourceLink.Builder {

        private final String name;
        private final String uri;
        private String title;
        private String description;
        private String mimeType;
        private Long size;

        ResourceLinkBuilderImpl(String name, String uri) {
            this.name = name;
            this.uri = uri;
        }

        @Override
        public ResourceLink.Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        @Override
        public ResourceLink.Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        @Override
        public ResourceLink.Builder setMimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        @Override
        public ResourceLink.Builder setAnnotations(Annotations a) {
            return super.setAnnotations(a);
        }

        @Override
        public ResourceLink.Builder setSize(long size) {
            this.size = size;
            return this;
        }

        @Override
        public ResourceLink build() {
            if (name == null || uri == null) {
                throw new IllegalStateException("name and uri are required for ResourceLink");
            }
            return new ResourceLinkImpl(
                    name,
                    title != null ? title : name,
                    uri,
                    Optional.ofNullable(description),
                    Optional.ofNullable(mimeType),
                    optAnnotations(),
                    size != null ? OptionalLong.of(size) : OptionalLong.empty(),
                    Map.copyOf(metadata));
        }
    }

    private static final class AnnotationsBuilderImpl implements Annotations.Builder {

        private Set<Role> audience;
        private double priority = Double.NaN;
        private Instant lastModified;

        @Override
        public Annotations.Builder setAudience(Role... roles) {
            this.audience = Set.of(roles);
            return this;
        }

        @Override
        public Annotations.Builder setAudience(Set<Role> roles) {
            this.audience = roles;
            return this;
        }

        @Override
        public Annotations.Builder setPriority(double priority) {
            this.priority = priority;
            return this;
        }

        @Override
        public Annotations.Builder setLastModified(Instant lastModified) {
            this.lastModified = lastModified;
            return this;
        }

        @Override
        public Annotations build() {
            return new AnnotationsImpl(
                    Optional.ofNullable(audience != null ? Set.copyOf(audience) : null),
                    Double.isNaN(priority) ? OptionalDouble.empty() : OptionalDouble.of(priority),
                    Optional.ofNullable(lastModified));
        }
    }

    private static final class PromptResponseBuilderImpl extends MetaBuilder<PromptResponse.Builder>
            implements PromptResponse.Builder {

        private String description;
        private final List<PromptMessage> messages = new ArrayList<>();

        @Override
        public PromptResponse.Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        @Override
        public PromptResponse.Builder addMessage(Role role, ContentBlock content) {
            messages.add(new PromptMessageImpl(role, content));
            return this;
        }

        @Override
        public PromptResponse build() {
            return new PromptResponseImpl(
                    Optional.ofNullable(description), List.copyOf(messages), Map.copyOf(metadata));
        }
    }

    private static final class TextResourceContentsBuilderImpl extends MetaBuilder<TextResourceContents.Builder>
            implements TextResourceContents.Builder {

        private final String uri;
        private final String text;
        private String mimeType;

        TextResourceContentsBuilderImpl(String uri, String text) {
            this.uri = uri;
            this.text = text;
        }

        @Override
        public TextResourceContents.Builder setMimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        @Override
        public TextResourceContents build() {
            return new TextResourceContentsImpl(uri, text, Optional.ofNullable(mimeType), Map.copyOf(metadata));
        }
    }

    private static final class BlobResourceContentsBuilderImpl extends MetaBuilder<BlobResourceContents.Builder>
            implements BlobResourceContents.Builder {

        private final String uri;
        private final byte[] data;
        private String mimeType;

        BlobResourceContentsBuilderImpl(String uri, byte[] data) {
            this.uri = uri;
            this.data = data != null ? data.clone() : null;
        }

        @Override
        public BlobResourceContents.Builder setMimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        @Override
        public BlobResourceContents build() {
            return new BlobResourceContentsImpl(uri, data, Optional.ofNullable(mimeType), Map.copyOf(metadata));
        }
    }

    private static final class ResourceResponseBuilderImpl extends MetaBuilder<ResourceResponse.Builder>
            implements ResourceResponse.Builder {

        private final List<ResourceContents> contents = new ArrayList<>();

        @Override
        public ResourceResponse.Builder addContents(ResourceContents c) {
            contents.add(c);
            return this;
        }

        @Override
        public ResourceResponse build() {
            return new ResourceResponseImpl(List.copyOf(contents), Map.copyOf(metadata));
        }
    }

    private static final class CompletionResultBuilderImpl extends MetaBuilder<CompletionResult.Builder>
            implements CompletionResult.Builder {

        private final List<String> values = new ArrayList<>();
        private int total = -1;
        private Boolean hasMore;

        @Override
        public CompletionResult.Builder addValue(String value) {
            values.add(value);
            return this;
        }

        @Override
        public CompletionResult.Builder addValues(Collection<String> vals) {
            values.addAll(vals);
            return this;
        }

        @Override
        public CompletionResult.Builder setTotal(int total) {
            this.total = total;
            return this;
        }

        @Override
        public CompletionResult.Builder setHasMore(Boolean hasMore) {
            this.hasMore = hasMore;
            return this;
        }

        @Override
        public CompletionResult build() {
            return new CompletionResultImpl(
                    List.copyOf(values),
                    total >= 0 ? OptionalInt.of(total) : OptionalInt.empty(),
                    Optional.ofNullable(hasMore),
                    Map.copyOf(metadata));
        }
    }

    private static final class ToolResponseBuilderImpl extends MetaBuilder<ToolResponse.Builder>
            implements ToolResponse.Builder {

        private final List<ContentBlock> content = new ArrayList<>();
        private Object structuredContent;
        private boolean isError;

        @Override
        public ToolResponse.Builder addContent(ContentBlock block) {
            content.add(block);
            return this;
        }

        @Override
        public ToolResponse.Builder addTextContent(String text) {
            content.add(TextContent.of(text));
            return this;
        }

        @Override
        public ToolResponse.Builder setStructuredContent(Object structured) {
            this.structuredContent = structured;
            return this;
        }

        @Override
        public ToolResponse.Builder setError(boolean error) {
            this.isError = error;
            return this;
        }

        @Override
        public ToolResponse build() {
            return new ToolResponseImpl(
                    List.copyOf(content), Optional.ofNullable(structuredContent), isError, Map.copyOf(metadata));
        }
    }

    private static final class IconBuilderImpl implements Icon.Builder {

        private final String src;
        private String mimeType;
        private final List<String> sizes = new ArrayList<>();
        private Icon.Theme theme;

        IconBuilderImpl(String src) {
            this.src = src;
        }

        @Override
        public Icon.Builder setMimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        @Override
        public Icon.Builder addSize(int w, int h) {
            sizes.add(w + "x" + h);
            return this;
        }

        @Override
        public Icon.Builder setAnySize() {
            sizes.add("any");
            return this;
        }

        @Override
        public Icon.Builder setTheme(Icon.Theme theme) {
            this.theme = theme;
            return this;
        }

        @Override
        public Icon build() {
            return new IconImpl(src, Optional.ofNullable(mimeType), List.copyOf(sizes), Optional.ofNullable(theme));
        }
    }

    // ---- SPI factory methods ----

    @Override
    public TextContent.Builder textContentBuilder(String text) {
        return new TextContentBuilderImpl(text);
    }

    @Override
    public AudioContent.Builder audioContentBuilder(byte[] data, String mimeType) {
        return new AudioContentBuilderImpl(data, mimeType);
    }

    @Override
    public ImageContent.Builder imageContentBuilder(byte[] data, String mimeType) {
        return new ImageContentBuilderImpl(data, mimeType);
    }

    @Override
    public EmbeddedResource.Builder textEmbeddedResourceBuilder(String text, String uri) {
        return new EmbeddedResourceBuilderImpl(text, uri);
    }

    @Override
    public EmbeddedResource.Builder blobEmbeddedResourceBuilder(byte[] data, String uri) {
        return new EmbeddedResourceBuilderImpl(data, uri);
    }

    @Override
    public ResourceLink.Builder resourceLinkBuilder(String name, String uri) {
        return new ResourceLinkBuilderImpl(name, uri);
    }

    @Override
    public Annotations.Builder annotationsBuilder() {
        return new AnnotationsBuilderImpl();
    }

    @Override
    public PromptResponse.Builder promptResponseBuilder() {
        return new PromptResponseBuilderImpl();
    }

    @Override
    public ResourceResponse.Builder resourceResponseBuilder() {
        return new ResourceResponseBuilderImpl();
    }

    @Override
    public TextResourceContents.Builder textResourceContentsBuilder(String uri, String text) {
        return new TextResourceContentsBuilderImpl(uri, text);
    }

    @Override
    public BlobResourceContents.Builder blobResourceContentsBuilder(String uri, byte[] data) {
        return new BlobResourceContentsBuilderImpl(uri, data);
    }

    @Override
    public ToolResponse.Builder toolResponseBuilder() {
        return new ToolResponseBuilderImpl();
    }

    @Override
    public CompletionResult.Builder completeResultBuilder() {
        return new CompletionResultBuilderImpl();
    }

    @Override
    public Icon.Builder iconBuilder(String src) {
        return new IconBuilderImpl(src);
    }
}
