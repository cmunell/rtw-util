package edu.cmu.ml.rtw.util;

/**
 * A simple class for reporting elapsed time.
 */
public class Timer {

    /**
     * The start time in milliseconds from the Epoch
     */
    private long startTime = 0;

    /**
     * The end time in milliseconds from the Epoch
     */
    private long endTime = 0;

    /**
     * (Re)starts the timer.
     */
    public void start() {
        startTime = System.currentTimeMillis();
        endTime = 0;
    }

    /**
     * Stops the timer.
     */
    public void stop() {
        endTime = System.currentTimeMillis();
    }

    /**
     * Returns the elapsed time using an automatically-chosen unit of time, and with precision past
     * the decimal point for anything other than milliseconds.
     *
     * Choice of the cutoff points for the units is somewhat arbitrary; feel free to adjust it.
     */
    public String getElapsedTimeFracAuto() {
        long ms = computeDeltaMilliSec();
        if (ms <= 3000) {
            return getElapsedTimeMilli();
        } else if (ms <= (100 * 1000)) {
            return getElapsedTimeFracSec();
        } else if (ms <= (30 * 60 * 1000)) {
            return getElapsedTimeFracMin();
        } else if (ms <= (36L * 60L * 60L * 1000L)) {
            return getElapsedTimeFracHr();
        } else {
            return getElapsedTimeFracDay();
        }
    }

    /**
     * Returns the elapsed time in days with two digits of precision past the decimal point
     */
    public String getElapsedTimeFracDay() {
        double deltaMin = computeDeltaSec();
        deltaMin /= 60.0;
        return String.format("%.2f day", deltaMin);
    }

    /**
     * Returns the elapsed time in hours with two digits of precision past the decimal point
     *
     * @return the elapsed time.
     */
    public String getElapsedTimeFracHr() {
        double deltaHr = computeDeltaSec();
        deltaHr /= (60.0 * 60.0);
        return String.format("%.2f hr", deltaHr);
    }

    /**
     * Returns the elapsed time in minutes with two digits of precision past the decimal point
     */
    public String getElapsedTimeFracMin() {
        double deltaMin = computeDeltaMilliSec();
        deltaMin /= (60.0 * 1000.0);
        return String.format("%.2f min", deltaMin);
    }

    /**
     * Returns the elapsed time in seconds with two digits of precision past the decimal point
     */
    public String getElapsedTimeFracSec() {
        double deltaSec = computeDeltaMilliSec();
        deltaSec /= 1000.0;
        return String.format("%.2f sec", deltaSec);
    }

    /**
     * Returns the elapsed time in whole milliseconds
     */
    public String getElapsedTimeMilli() {
        long ms = computeDeltaMilliSec();
        return ms + " ms";
    }

    /**
     * Returns the elapsed time in the following format: "XX hr YY min".
     * 
     * @return the elapsed time.
     */
    public String getElapsedTimeHrMin() {
        long deltaMin = computeDeltaSec() / 60;
        long deltaHr = deltaMin / 60;

        return deltaHr + " hr " + deltaMin % 60 + " min";
    }

    /**
     * Returns the elapsed time in minutes.
     * 
     * @return the elapsed time.
     */
    public int getElapsedMin() {
        long deltaMin = computeDeltaSec() / 60;
        return (int) deltaMin;
    }

    /**
     * Returns the elapsed time in seconds.
     *
     * @return the elapsed time.
     */
    public int getElapsedSeconds() {
        long deltaSec = computeDeltaSec();
        return (int) deltaSec;
    }

    public int getElapsedMilliseconds() {
        long deltaMilliSec = computeDeltaMilliSec();
        return (int) deltaMilliSec;
    }

    private long computeDeltaSec() {
        return computeDeltaMilliSec() / 1000;
    }

    private long computeDeltaMilliSec() {
        long end = 0;
        if (startTime == 0)
            throw new IllegalStateException(
                    "Must start the timer before requesting the elapsed time.");
        if (endTime == 0)
            end = System.currentTimeMillis();
        else
            end = endTime;

        return (end - startTime);
    }

}
