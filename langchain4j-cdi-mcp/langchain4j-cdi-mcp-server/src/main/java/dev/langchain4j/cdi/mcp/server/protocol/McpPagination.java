package dev.langchain4j.cdi.mcp.server.protocol;

import java.util.Base64;
import java.util.List;

/** Utility for cursor-based pagination. Cursors are base64-encoded offsets. */
public final class McpPagination {

    public static final int DEFAULT_PAGE_SIZE = 50;

    private McpPagination() {}

    public static int decodeOffset(String cursor) {
        if (cursor == null || cursor.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(new String(Base64.getDecoder().decode(cursor)));
        } catch (Exception e) {
            return 0;
        }
    }

    public static String encodeCursor(int offset) {
        return Base64.getEncoder().encodeToString(Integer.toString(offset).getBytes());
    }

    public static <T> Page<T> paginate(List<T> items, String cursor) {
        return paginate(items, cursor, DEFAULT_PAGE_SIZE);
    }

    public static <T> Page<T> paginate(List<T> items, String cursor, int pageSize) {
        int offset = decodeOffset(cursor);
        int end = Math.min(offset + pageSize, items.size());
        List<T> page = items.subList(Math.min(offset, items.size()), end);
        String nextCursor = end < items.size() ? encodeCursor(end) : null;
        return new Page<>(page, nextCursor);
    }

    public record Page<T>(List<T> items, String nextCursor) {}
}
