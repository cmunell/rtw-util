package edu.cmu.ml.rtw.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class LogTest { 
    private final static Logger log = LogFactory.getLogger();

    public static void main(String[] args) {
        //log.debug("Hello, debug");
        log.info("Hello, info");
        //log.error("Hello, error");
        //log.fatal("Hello, fatal");
    }
}