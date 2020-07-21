package web;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import web.message.MessageHandler;

import java.net.InetSocketAddress;
import java.util.HashSet;

public class WebServer extends WebSocketServer {
    private static final Logger logger = LoggerFactory.getLogger(WebServer.class);
    private final HashSet<WebSocket> sockets;

    WebServer(int port) {
        super(new InetSocketAddress(port));
        sockets = new HashSet<>();
    }

    /**
     * Called after an opening handshake has been performed and the given websocket is ready to be written on.
     *
     * @param socket    The <tt>WebSocket</tt> instance this event is occuring on.
     * @param handshake The handshake of the websocket instance
     */
    @Override
    public void onOpen(WebSocket socket, ClientHandshake handshake) {
        sockets.add(socket);
        WebServer.logger.info("Connection established from: " + WebServer.getSocketAddress(socket));
    }

    /**
     * Called after the websocket connection has been closed.
     *
     * @param socket The <tt>WebSocket</tt> instance this event is occurring on.
     * @param code   The codes can be looked up here: {@link CloseFrame}
     * @param reason Additional information string
     * @param remote If remote
     **/
    @Override
    public void onClose(WebSocket socket, int code, String reason, boolean remote) {
        if (code == CloseFrame.NORMAL || code == CloseFrame.GOING_AWAY) {
            WebServer.logger.info("Gracefully closed connection.\n"
                    + "Address: " + WebServer.getSocketAddress(socket));
        }
        else {
            WebServer.logger.warn("Connection closed unexpectedly.\n"
                    + "Address: " + WebServer.getSocketAddress(socket) + "\n"
                    + "Error code: " + code
                    + "Reason: " + reason);
        }
        sockets.remove(socket);
    }

    /**
     * Callback for string messages received from the remote host
     *
     * @param socket  The <tt>WebSocket</tt> instance this event is occurring on.
     * @param message The UTF-8 decoded message that was received.
     **/
    @Override
    public void onMessage(WebSocket socket, String message) {
        MessageHandler.Handle(message);
    }

    public void broadcastMessage(String message) {
        for (var socket : sockets) {
            socket.send(message);
        }
    }

    /**
     * Called when errors occurs. If an error causes the websocket connection to fail {@link #onClose(WebSocket, int, String, boolean)}
     * will be called additionally. <br>
     * This method will be called primarily because of IO or protocol errors.<br>
     * If the given exception is an RuntimeException that probably means that you encountered a bug.<br>
     *
     * @param socket Can be null if there error does not belong to one specific websocket. For example if the servers port could not be bound.
     * @param ex     The exception causing this error
     **/
    @Override
    public void onError(WebSocket socket, Exception ex) {
        WebServer.logger.error("Error in connection to " + WebServer.getSocketAddress(socket), ex);
        sockets.remove(socket);
    }

    /**
     * Called when the server started up successfully.
     * <p>
     * If any error occurred, onError is called instead.
     */
    @Override
    public void onStart() {
        WebServer.logger.info("Successfully started.");
    }

    private static String getSocketAddress(WebSocket socket) {
        return socket.getRemoteSocketAddress().getHostString();
    }
}
