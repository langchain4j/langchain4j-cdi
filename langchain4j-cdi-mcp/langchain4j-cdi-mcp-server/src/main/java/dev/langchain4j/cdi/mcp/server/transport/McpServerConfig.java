package dev.langchain4j.cdi.mcp.server.transport;

public class McpServerConfig {

    private String serverName = "langchain4j-cdi";
    private String serverVersion = "unknown";

    public McpServerConfig() {}

    public McpServerConfig(String serverName, String serverVersion) {
        this.serverName = serverName;
        this.serverVersion = serverVersion;
    }

    public static McpServerConfigBuilder builder() {
        return new McpServerConfigBuilder();
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public void setServerVersion(String serverVersion) {
        this.serverVersion = serverVersion;
    }

    public static class McpServerConfigBuilder {

        private String serverName = "langchain4j-cdi";
        private String serverVersion = "unknown";

        public McpServerConfigBuilder serverName(String serverName) {
            this.serverName = serverName;
            return this;
        }

        public McpServerConfigBuilder serverVersion(String serverVersion) {
            this.serverVersion = serverVersion;
            return this;
        }

        public McpServerConfig build() {
            return new McpServerConfig(serverName, serverVersion);
        }
    }
}
