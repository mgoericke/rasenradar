package de.javamark.matchoracle.forecast.control;

import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.quarkiverse.langchain4j.ModelName;
import jakarta.enterprise.inject.spi.CDI;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import java.util.Optional;
import java.util.Set;

/**
 * Tries the hosted model first and falls back to the local one when the hosted
 * call fails — unreachable, no or invalid key, credits used up, timeout. Where no
 * local model runs (production), the fallback is switched off so the hosted
 * model's own error surfaces instead of a "connection refused" from a port nobody
 * listens on. Which model answered is recorded in {@link ModelUsage} and ends up
 * on the forecast.
 */
public final class FallbackChatModel implements ChatModel {

    private static final Logger LOG = Logger.getLogger(FallbackChatModel.class);

    private final Optional<ChatModel> cloud;
    private final String cloudName;
    private final Optional<ChatModel> local;
    private final String localName;
    private final ModelUsage usage;

    FallbackChatModel(Optional<ChatModel> cloud, String cloudName, Optional<ChatModel> local, String localName, ModelUsage usage) {
        if (cloud.isEmpty() && local.isEmpty()) {
            throw new IllegalStateException("No chat model left: cloud and local fallback are both disabled");
        }
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
        boolean localEnabled = config.getOptionalValue("matchoracle.forecast.local-fallback-enabled", Boolean.class).orElse(true);
        Optional<ChatModel> cloud = cloudEnabled
                ? Optional.of(CDI.current().select(ChatModel.class, ModelName.Literal.of("cloud")).get())
                : Optional.empty();
        Optional<ChatModel> local = localEnabled
                ? Optional.of(CDI.current().select(ChatModel.class).get())
                : Optional.empty();
        return new FallbackChatModel(cloud,
                config.getValue("quarkus.langchain4j.anthropic.cloud.chat-model.model-name", String.class),
                local,
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
                return repaired(request, response);
            } catch (RuntimeException e) {
                if (local.isEmpty()) {
                    LOG.warnf("Cloud model %s failed (%s), no local fallback configured", cloudName, ForecastService.rootMessage(e));
                    throw e;
                }
                LOG.warnf("Cloud model %s failed (%s), falling back to %s", cloudName, ForecastService.rootMessage(e), localName);
            }
        }
        ChatResponse response = local.orElseThrow().chat(request);
        usage.record(cloud.isPresent() ? localName + " (Fallback)" : localName);
        return repaired(request, response);
    }

    /** A JSON answer the model could not close properly is salvaged before the AI service parses it, see {@link JsonAnswerRepair}. */
    private static ChatResponse repaired(ChatRequest request, ChatResponse response) {
        if (request.responseFormat() == null || request.responseFormat().type() != ResponseFormatType.JSON
                || response.aiMessage() == null || response.aiMessage().text() == null) {
            return response;
        }
        String text = response.aiMessage().text();
        String repaired = JsonAnswerRepair.repair(text);
        if (repaired == text) {
            return response;
        }
        return ChatResponse.builder().aiMessage(AiMessage.from(repaired)).metadata(response.metadata()).build();
    }

    @Override
    public ChatRequestParameters defaultRequestParameters() {
        return primary().defaultRequestParameters();
    }

    /**
     * Structured output: with this capability the AI service sends the JSON schema as a
     * response format instead of describing it in the prompt — no more typographic quotes
     * breaking the JSON. Both models support schema-based JSON.
     */
    @Override
    public Set<Capability> supportedCapabilities() {
        return primary().supportedCapabilities();
    }

    private ChatModel primary() {
        return cloud.or(() -> local).orElseThrow();
    }
}
