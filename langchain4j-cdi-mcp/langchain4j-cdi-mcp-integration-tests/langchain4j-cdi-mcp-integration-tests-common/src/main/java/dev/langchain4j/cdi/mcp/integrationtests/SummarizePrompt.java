package dev.langchain4j.cdi.mcp.integrationtests;

import jakarta.enterprise.context.ApplicationScoped;
import org.mcpjava.server.prompts.Prompt;
import org.mcpjava.server.prompts.PromptArg;

/** MCP prompt that generates a summarization request. */
@ApplicationScoped
public class SummarizePrompt {

    /** Creates a new instance. */
    public SummarizePrompt() {}

    /**
     * Produces a summarization prompt for the given text.
     *
     * @param text the text to summarize
     * @return the summarization prompt
     */
    @Prompt(description = "Summarize the given text")
    public String summarize(@PromptArg(description = "The text to summarize") String text) {
        return "Please summarize the following text:\n\n" + text;
    }
}
