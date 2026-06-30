package dev.langchain4j.cdi.example.chat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import java.util.logging.Logger;

/** WebSocket endpoint for chat interactions. */
@ApplicationScoped
@ServerEndpoint(
        value = "/chat",
        encoders = {ChatMessageEncoder.class})
public class ChatService {

    private static Logger logger = Logger.getLogger(ChatService.class.getName());

    @Inject
    ChatAgent agent = null;

    /** Creates a new chat service. */
    public ChatService() {}

    /**
     * Handles a new WebSocket connection.
     *
     * @param session the WebSocket session
     */
    @OnOpen
    public void onOpen(Session session) {
        logger.info("Server connected to session: " + session.getId());
    }

    /**
     * Handles an incoming WebSocket message.
     *
     * @param message the message text
     * @param session the WebSocket session
     */
    @OnMessage
    public void onMessage(String message, Session session) {

        logger.info("Server received message \"" + message + "\" " + "from session: " + session.getId());

        String answer;
        try {
            String sessionId = session.getId();
            answer = agent.chat(sessionId, message);
        } catch (Exception e) {
            answer = "My failure reason is:\n\n" + e.getMessage();
        }

        try {
            session.getBasicRemote().sendObject(answer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles a WebSocket connection close.
     *
     * @param session the WebSocket session
     * @param closeReason the close reason
     */
    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.info("Session " + session.getId() + " was closed with reason " + closeReason.getCloseCode());
    }

    /**
     * Handles a WebSocket error.
     *
     * @param session the WebSocket session
     * @param throwable the error
     */
    @OnError
    public void onError(Session session, Throwable throwable) {
        logger.info("WebSocket error for " + session.getId() + " " + throwable.getMessage());
    }
}
