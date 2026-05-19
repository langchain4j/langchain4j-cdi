package dev.langchain4j.cdi.mcp.server.api;

import dev.langchain4j.cdi.mcp.server.transport.McpSamplingManager;
import dev.langchain4j.cdi.mcp.server.transport.McpSession;
import org.mcp_java.server.sampling.ModelPreferences;
import org.mcp_java.server.sampling.Sampling;
import org.mcp_java.server.sampling.SamplingRequest;

/** Implementation of {@link Sampling} that delegates to {@link McpSamplingManager}. */
public class CdiSampling implements Sampling {

    private final McpSession session;
    private final McpSamplingManager samplingManager;
    private final String sessionId;

    public CdiSampling(McpSession session, McpSamplingManager samplingManager, String sessionId) {
        this.session = session;
        this.samplingManager = samplingManager;
        this.sessionId = sessionId;
    }

    @Override
    public boolean isSupported() {
        return session.hasCapability("sampling");
    }

    @Override
    public SamplingRequest.Builder requestBuilder() {
        return new CdiSamplingRequest.CdiBuilder(samplingManager, sessionId);
    }

    @Override
    public ModelPreferences.Builder modelPreferenceBuilder() {
        return new CdiModelPreferencesBuilder();
    }

    private static class CdiModelPreferencesBuilder implements ModelPreferences.Builder {

        private final java.util.List<String> names = new java.util.ArrayList<>();
        private double costPriority = Double.NaN;
        private double speedPriority = Double.NaN;
        private double intelligencePriority = Double.NaN;

        @Override
        public ModelPreferences.Builder addName(String name) {
            names.add(name);
            return this;
        }

        @Override
        public ModelPreferences.Builder setCostPriority(double priority) {
            this.costPriority = priority;
            return this;
        }

        @Override
        public ModelPreferences.Builder setSpeedPriority(double priority) {
            this.speedPriority = priority;
            return this;
        }

        @Override
        public ModelPreferences.Builder setIntelligencePriority(double priority) {
            this.intelligencePriority = priority;
            return this;
        }

        @Override
        public ModelPreferences build() {
            java.util.List<String> namesCopy = java.util.List.copyOf(names);
            double cost = costPriority;
            double speed = speedPriority;
            double intelligence = intelligencePriority;
            return new ModelPreferences() {
                @Override
                public java.util.List<String> names() {
                    return namesCopy;
                }

                @Override
                public java.util.OptionalDouble costPriority() {
                    return Double.isNaN(cost) ? java.util.OptionalDouble.empty() : java.util.OptionalDouble.of(cost);
                }

                @Override
                public java.util.OptionalDouble speedPriority() {
                    return Double.isNaN(speed) ? java.util.OptionalDouble.empty() : java.util.OptionalDouble.of(speed);
                }

                @Override
                public java.util.OptionalDouble intelligencePriority() {
                    return Double.isNaN(intelligence)
                            ? java.util.OptionalDouble.empty()
                            : java.util.OptionalDouble.of(intelligence);
                }
            };
        }
    }
}
