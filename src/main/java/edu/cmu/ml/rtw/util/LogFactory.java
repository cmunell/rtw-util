package edu.cmu.ml.rtw.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * General way to obtain some appropriate {@link Logger} instance
 *
 * This is meant to factor out the question of which concrete Logger implementation to use so that
 * dependency on log4j (or whatever else) is easily controlled in some global fashion.  The
 * expectation is for Theo2012 code to simply use the {@link getLogger} method in order to achieve
 * the appropriate policy.
 *
 * Initially, this uses log4j if and only if the "log4j.configurationFile" system property is set.
 * If no such property is set, then logging is disabled and there will be no need to have log4j
 * loadable from the classpath.  The important design goal here is that logging be unobstrusive and
 * not require dependency on 3rd party libraries unless doing otherwise is explicitly turned on
 * programatically or by some configureation.  We can get more sophisticated as needed.
 *
 * If the "log4j.configurationFile" is not set but "log4j.console" is set to "true" then a built-in
 * logging implementation will be used that echos to the console.  This can be useful for small
 * utilities and suchlike where echoing messages to the console is desirable without having log4j as
 * a dependency unless the user has configured it.
 */
public class LogFactory { 
    public static Logger getNullLogger() {
        return new NullLogger();
    }

    public static Logger getConsoleLogger() {
        return new ConsoleLogger();
    }

    public static Logger getL4JLogger() {
        return new L4JLogger();
    }

    public static Logger getLogger() {
        if (System.getProperty("log4j.configurationFile") == null) {
            String console = System.getProperty("log4j.console");
            if (console != null && console.equals("true"))
                return getConsoleLogger();
            else 
                return getNullLogger();
        } else {
            return getL4JLogger();
        }
    }
}