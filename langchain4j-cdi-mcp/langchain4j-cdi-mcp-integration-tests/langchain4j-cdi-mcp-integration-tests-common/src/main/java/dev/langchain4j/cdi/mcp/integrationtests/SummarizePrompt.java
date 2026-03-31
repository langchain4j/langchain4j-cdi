package dev.langchain4j.cdi.mcp.integrationtests;

import jakarta.enterprise.context.ApplicationScoped;
import org.mcp_java.annotations.prompts.Prompt;
import org.mcp_java.annotations.prompts.PromptArg;

@ApplicationScoped
public class SummarizePrompt {

    @Prompt(description = "Summarize the given text")
    public String summarize(@PromptArg(description = "The text to summarize") String text) {
        return "Please summarize the following text:\n\n" + text;
    }
}
