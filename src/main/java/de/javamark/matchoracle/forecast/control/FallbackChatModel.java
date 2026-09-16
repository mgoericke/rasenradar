package de.javamark.matchoracle.forecast.control;

import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.quarkiverse.langchain4j.ModelName;
import jakarta.enterprise.inject.spi.CDI;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import java.util.Optional;
import java.util.Set;

/**
 * Tries the hosted model first and falls back to the local one when the hosted
 * call fails — unreachable, no or invalid key, credits used up, timeout. Which
 * model answered is recorded in {@link ModelUsage} and ends up on the forecast.
 */
public final class FallbackChatModel implements ChatModel {

    private static final Logger LOG = Logger.getLogger(FallbackChatModel.class);

    private final Optional<ChatModel> cloud;
    private final String cloudName;
    private final ChatModel local;
    private final String localName;
    private final ModelUsage usage;

    FallbackChatModel(Optional<ChatModel> cloud, String cloudName, ChatModel local, String localName, ModelUsage usage) {
        this.cloud = cloud;
        this.cloudName = cloudName;
        this.local = local;
        this.localName = localName;
        this.usage = usage;
    }

    /** Used by the agents' {@code @ChatModelSupplier} methods; resolved from CDI because those methods are static. */
    static ChatModel resilient() {
        var config = ConfigProvider.getConfig();
        boolean cloudEnabled = config.getOptionalValue("matchoracle.forecast.cloud-enabled", Boolean.class).orElse(true);
        Optional<ChatModel> cloud = cloudEnabled
                ? Optional.of(CDI.current().select(ChatModel.class, ModelName.Literal.of("cloud")).get())
                : Optional.empty();
        return new FallbackChatModel(cloud,
                config.getValue("quarkus.langchain4j.anthropic.cloud.chat-model.model-name", String.class),
                CDI.current().select(ChatModel.class).get(),
                config.getValue("quarkus.langchain4j.ollama.chat-model.model-name", String.class),
                CDI.current().select(ModelUsage.class).get());
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        if (cloud.isPresent()) {
            try {
                LOG.debugf("Asking cloud model %s", cloudName);
                ChatResponse response = cloud.get().chat(request);
                usage.record(cloudName);
                return response;
            } catch (RuntimeException e) {
                LOG.warnf("Cloud model %s failed (%s), falling back to %s", cloudName, rootMessage(e), localName);
            }
        }
        ChatResponse response = local.chat(request);
        usage.record(cloud.isPresent() ? localName + " (Fallback)" : localName);
        return response;
    }

    @Override
    public ChatRequestParameters defaultRequestParameters() {
        return cloud.orElse(local).defaultRequestParameters();
    }

    /**
     * Structured output: with this capability the AI service sends the JSON schema as a
     * response format instead of describing it in the prompt — no more typographic quotes
     * breaking the JSON. Both models support schema-based JSON.
     */
    @Override
    public Set<Capability> supportedCapabilities() {
        return cloud.orElse(local).supportedCapabilities();
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        String m = t.getMessage();
        return m == null ? t.getClass().getSimpleName() : m.length() > 200 ? m.substring(0, 200) : m;
    }
}
