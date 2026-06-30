package dev.langchain4j.cdi.example.chat;

import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;

/** WebSocket encoder for chat messages. */
public class ChatMessageEncoder implements Encoder.Text<String> {

    /** Creates a new chat message encoder. */
    public ChatMessageEncoder() {}

    /** {@inheritDoc} */
    @Override
    public String encode(String message) throws EncodeException {

        if (!message.endsWith(".")) {
            message += " ...";
        }

        message = message.replaceAll("\n", "<br/>");

        return message;
    }
}
