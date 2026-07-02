package dev.langchain4j.cdi.mcp.server.transport;

/** Configuration holder for MCP server identity, providing the server name and version used during initialization. */
public class McpServerConfig {

    /** The server name advertised to clients. Defaults to {@code "langchain4j-cdi"}. */
    private String serverName = "langchain4j-cdi";

    /** The server version advertised to clients. Defaults to {@code "unknown"}. */
    private String serverVersion = "unknown";

    /** Creates a config with default server name and version. */
    public McpServerConfig() {}

    /**
     * Creates a config with the specified server name and version.
     *
     * @param serverName the server name
     * @param serverVersion the server version
     */
    public McpServerConfig(String serverName, String serverVersion) {
        this.serverName = serverName;
        this.serverVersion = serverVersion;
    }

    /**
     * Returns a new builder for constructing an {@link McpServerConfig}.
     *
     * @return a new builder instance
     */
    public static McpServerConfigBuilder builder() {
        return new McpServerConfigBuilder();
    }

    /**
     * Returns the server name.
     *
     * @return the server name
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * Sets the server name.
     *
     * @param serverName the server name
     */
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    /**
     * Returns the server version.
     *
     * @return the server version
     */
    public String getServerVersion() {
        return serverVersion;
    }

    /**
     * Sets the server version.
     *
     * @param serverVersion the server version
     */
    public void setServerVersion(String serverVersion) {
        this.serverVersion = serverVersion;
    }

    /** Builder for constructing {@link McpServerConfig} instances. */
    public static class McpServerConfigBuilder {

        private String serverName = "langchain4j-cdi";
        private String serverVersion = "unknown";

        /** Creates a new builder with default values. */
        public McpServerConfigBuilder() {}

        /**
         * Sets the server name.
         *
         * @param serverName the server name
         * @return this builder
         */
        public McpServerConfigBuilder serverName(String serverName) {
            this.serverName = serverName;
            return this;
        }

        /**
         * Sets the server version.
         *
         * @param serverVersion the server version
         * @return this builder
         */
        public McpServerConfigBuilder serverVersion(String serverVersion) {
            this.serverVersion = serverVersion;
            return this;
        }

        /**
         * Builds the {@link McpServerConfig}.
         *
         * @return the constructed config
         */
        public McpServerConfig build() {
            return new McpServerConfig(serverName, serverVersion);
        }
    }
}
