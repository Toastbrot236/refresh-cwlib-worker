package refresh.server.cwlib;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.sql.SQLException;

public class Main {
    private static final Logger logger = LogManager.getLogger();
    private static CwlibServer Server;

    public static void main(String[] args) throws SQLException {
        // setup logging stuffs
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        logger.info("Initializing configs...");
        // todo

        logger.info("Starting up server...");
        Server = new CwlibServer();
    }
}