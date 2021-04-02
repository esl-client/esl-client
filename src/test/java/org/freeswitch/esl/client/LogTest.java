package org.freeswitch.esl.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogTest {

    private static final Logger logger = LoggerFactory.getLogger(LogTest.class);

    public static void main(String[] args) {
        logger.debug("debug log");
        logger.info("info log");
        logger.warn("warn log");
    }
}
