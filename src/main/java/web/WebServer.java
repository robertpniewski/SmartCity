package web;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

public class WebServer extends WebSocketServer {
    private static final Logger logger = LoggerFactory.getLogger(WebServer.class);
    private Set<WebSocket> conns;

    public WebServer(int port) {
        super(new InetSocketAddress(port));
        conns = new HashSet<>();
    }

    /**
     * Called after an opening handshake has been performed and the given websocket is ready to be written on.
     *
     * @param conn      The <tt>WebSocket</tt> instance this event is occuring on.
     * @param handshake The handshake of the websocket instance
     */
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        conns.add(conn);
        logger.info("Connection established from: " + conn.getRemoteSocketAddress().getHostString());
    }

    /**
     * Called after the websocket connection has been closed.
     *
     * @param conn   The <tt>WebSocket</tt> instance this event is occuring on.
     * @param code   The codes can be looked up here: {@link CloseFrame}
     * @param reason Additional information string
     * @param remote If remote
     **/
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {

    }

    /**
     * Callback for string messages received from the remote host
     *
     * @param conn    The <tt>WebSocket</tt> instance this event is occuring on.
     * @param message The UTF-8 decoded message that was received.
     **/
    @Override
    public void onMessage(WebSocket conn, String message) {

    }

    /**
     * Called when errors occurs. If an error causes the websocket connection to fail {@link #onClose(WebSocket, int, String, boolean)} will be called additionally.<br>
     * This method will be called primarily because of IO or protocol errors.<br>
     * If the given exception is an RuntimeException that probably means that you encountered a bug.<br>
     *
     * @param conn Can be null if there error does not belong to one specific websocket. For example if the servers port could not be bound.
     * @param ex   The exception causing this error
     **/
    @Override
    public void onError(WebSocket conn, Exception ex) {
        logger.error("Error when connecting", ex);
    }

    /**
     * Called when the server started up successfully.
     * <p>
     * If any error occured, onError is called instead.
     */
    @Override
    public void onStart() {
        logger.info("Successfully started.");
    }
}
