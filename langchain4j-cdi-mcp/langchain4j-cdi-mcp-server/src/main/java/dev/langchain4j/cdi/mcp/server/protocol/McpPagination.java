package dev.langchain4j.cdi.mcp.server.protocol;

import java.util.Base64;
import java.util.List;

/** Utility for cursor-based pagination. Cursors are base64-encoded offsets. */
public final class McpPagination {

    /** Default number of items per page. */
    public static final int DEFAULT_PAGE_SIZE = 50;

    private McpPagination() {}

    /**
     * Decodes a base64-encoded cursor into a numeric offset.
     *
     * @param cursor the base64-encoded cursor, or {@code null}/empty to start from the beginning
     * @return the decoded offset, or {@code 0} if the cursor is absent or invalid
     */
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

    /**
     * Encodes an offset into a base64 cursor string.
     *
     * @param offset the numeric offset to encode
     * @return the base64-encoded cursor
     */
    public static String encodeCursor(int offset) {
        return Base64.getEncoder().encodeToString(Integer.toString(offset).getBytes());
    }

    /**
     * Paginates a list using the {@link #DEFAULT_PAGE_SIZE default page size}.
     *
     * @param <T> the element type
     * @param items the full list of items
     * @param cursor the base64-encoded cursor, or {@code null} to start from the beginning
     * @return a page containing the items and an optional next cursor
     */
    public static <T> Page<T> paginate(List<T> items, String cursor) {
        return paginate(items, cursor, DEFAULT_PAGE_SIZE);
    }

    /**
     * Paginates a list with a specified page size.
     *
     * @param <T> the element type
     * @param items the full list of items
     * @param cursor the base64-encoded cursor, or {@code null} to start from the beginning
     * @param pageSize the maximum number of items per page
     * @return a page containing the items and an optional next cursor
     */
    public static <T> Page<T> paginate(List<T> items, String cursor, int pageSize) {
        int offset = decodeOffset(cursor);
        int end = Math.min(offset + pageSize, items.size());
        List<T> page = items.subList(Math.min(offset, items.size()), end);
        String nextCursor = end < items.size() ? encodeCursor(end) : null;
        return new Page<>(page, nextCursor);
    }

    /**
     * A single page of results with an optional cursor to the next page.
     *
     * @param <T> the element type
     * @param items the items in this page
     * @param nextCursor the cursor for the next page, or {@code null} if this is the last page
     */
    public record Page<T>(List<T> items, String nextCursor) {}
}
