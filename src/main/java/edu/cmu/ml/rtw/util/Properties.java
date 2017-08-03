package edu.cmu.ml.rtw.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

/**
 * Properties class the way it was meant to be; Generics and property filtering
 * and manipulation based on the conventional dot-separator property name
 * syntax.
 * 
 * @author hazen
 *
 *
 * - Added a much needed constructor: make a new properties object from a
 * Map<String,String>.
 * - Changed extending from HashMap to TreeMap because TreeMaps maintain sorted order.
 * Moreover, we won't see any performance increases from using a HashMap in this scenario.
 * @author Malcolm Greaves (mwgreaves@cmu.edu)
 */
public class Properties extends TreeMap<String, String> {
        private final static Logger log = LogFactory.getLogger();

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

    /**
     * Used by {@link setPerFileOverride} et al
     */
    protected static Map<String, Map<String, String>> perFileOverrides =
            new HashMap<String, Map<String, String>>();

	public Properties() {
		super();
	}

	public Properties(Properties properties) {
		super(properties);
	}

	/**
	 * Instantiates the Properties object with the key-value mapping
	 * in the parameter propertiesMap.
	 **/
	public Properties(Map<String,String> propertiesMap){
		load(propertiesMap);	
	}	

    /**
     * When properties are loaded from the given file name (by {@link loadProperties} or {@link
     * loadFromClassName}), the given overrides will be applied.
     *
     * The purpose of this system is to address situations where properties settings need to come
     * from somewhere other than the usual properties files.  It's a somewhat provisional system in
     * that we'll eventually need to overhaul our current way of handling external settings, but the
     * hope is that it's simple, extensible, and durable enough to live with amicably for the time
     * being.
     *
     * Note that {@link loadProperties} will search the CLASSPATH for properties files.  {@link
     * setPerFileOverride} is for situations that lack properties files or that need hypervisory
     * modification of their contents.
     *
     * Note that many classes load their properties by way of static initialization.  Naturally,
     * this presents a problem: their overrides must be set before that static initialization takes
     * place.  Static initialization in general leads to difficult situations that are not easy to
     * control or coordinate, and "real" solutions to this problem may well have to come in the form
     * of removing staticness.  Writing static code is such a common and convenient thing to do,
     * though, that it becomes unrealistic to expect that nobody will ever have static
     * initialization of their properties.  So the best we hope for here is that a situation that
     * needs to use this functionality will have enough control over whatever is running and its own
     * JVM to be able to set these overrides before the target classes are loaded.
     *
     * These overrides are applied after the properties are loaded from the file but before any
     * caller-supplied overrides.  Thus, the effects of {@link setPerFileOverride} are
     * indistinguishable from modifying the properties files themselves.
     *
     * If overrides is null, the effect will be to delete any overrides currently set for the given
     * filename.
     *
     * If the value specified for a given property is null, that will be interpreted as an override
     * that causes the given property to have no value, as if it had never been set to anything.
     */
    public static void setPerFileOverride(String propertyFileName, Map<String, String> overrides) {  // bk:prop
        if (overrides == null)
            perFileOverrides.remove(propertyFileName);
        else
            perFileOverrides.put(propertyFileName, overrides);
    }

    /**
     * Get overrides set for the given filename by {@link setPerFileOverride}
     */
    public static Map<String, String> getPerFileOverride(String propertyFileName) {
        return perFileOverrides.get(propertyFileName);
    }

	public String getProperty(String name) {
            try {
                String v = get(name);
                if (v == null) return null;
                v = v.trim();
                if (v.charAt(v.length() - 1) == ';')
                    log.warn("Property \"" + name + " = " + v
                            + "\" may be misinterpreted due to trailing semicolon");
                return v;
            } catch (Exception e) {
                throw new RuntimeException("getProperty(\"" + name + "\")", e);
            }
	}

	public void setProperty(String name, String value) {
            put(name, value);
	}

	public void setProperty(String name, Integer value) {
            put(name, value.toString());
	}

	public void setProperty(String name, Double value) {
            put(name, value.toString());
	}

	public void setProperty(String name, Boolean value) {
            put(name, value.toString());
	}

	public String getProperty(String name, String defaultValue) {
                String value = getProperty(name);
		if (value == null)
			return defaultValue;
		return value;
	}

	public Set<String> getPropertyNames() {
		return keySet();
	}

	public int getPropertyIntegerValue(String key) {
		try {
			String value = getProperty(key);
			if (value == null)
				throw new RuntimeException("Missing integer value for property \"" + key + "\"");
			return Integer.valueOf(value);
		} catch (Exception e) {
			throw new RuntimeException("getPropertyIntegerValue(\"" + key + "\")", e);
		}
	}

	public Integer getPropertyIntegerValue(String key, Integer defaultValue) {
		try {
			String value = getProperty(key);
			if (value == null)
				return defaultValue;
			return Integer.valueOf(value);
		} catch (Exception e) {
			throw new RuntimeException("getPropertyIntegerValue(\"" + key + "\", " + defaultValue
					+ ")", e);
		}
	}

	public double getPropertyDoubleValue(String key) {
		try {
			String value = getProperty(key);
			if (value == null)
				throw new RuntimeException("Missing double value for property \"" + key + "\"");
			return Double.valueOf(value);
		} catch (Exception e) {
			throw new RuntimeException("getPropertyDoubleValue(\"" + key + "\")", e);
		}
	}

	public Double getPropertyDoubleValue(String key, Double defaultValue) {
		try {
			String value = getProperty(key);
			if (value == null)
				return defaultValue;
			return Double.valueOf(value);
		} catch (Exception e) {
			throw new RuntimeException("getPropertyDoubleValue(\"" + key + "\", " + defaultValue
					+ ")", e);
		}
	}        

	public boolean getPropertyBooleanValue(String key) {
		try {
			String value = getProperty(key);
			if (value == null)
				throw new RuntimeException("Missing boolean value for property \"" + key + "\"");

                        // bkisiel 2015-09-16: not using Boolean.valueOf because it is too tolerant
                        // (e.g. "asfasdfadf" is false) and there's no good reason why we shouldn't
                        // consider it a useful error condition for a boolean-valued property to be
                        // anything other than "true" or "false"
                        if (value.equals("true")) return true;
                        if (value.equals("false")) return false;
                        throw new RuntimeException("Illegal value \"" + value
                                + "\" for boolean-valued property \"" + key + "\"");
		} catch (Exception e) {
			throw new RuntimeException("getPropertyBooleanValue(\"" + key + "\")", e);
		}
	}

	public Boolean getPropertyBooleanValue(String key, Boolean defaultValue) {
		try {
			String value = getProperty(key);
			if (value == null)
				return defaultValue;

                        // bkisiel 2015-09-16: not using Boolean.valueOf because it is too tolerant
                        // (e.g. "asfasdfadf" is false) and there's no good reason why we shouldn't
                        // consider it a useful error condition for a boolean-valued property to be
                        // anything other than "true" or "false"
                        if (value.equals("true")) return true;
                        if (value.equals("false")) return false;
                        throw new RuntimeException("Illegal value \"" + value
                                + "\" for boolean-valued property \"" + key + "\"");
		} catch (Exception e) {
			throw new RuntimeException("getPropertyBooleanValue(\"" + key + "\", " + defaultValue
					+ ")", e);
		}
	}

	public int[] getPropertyIntegerValueArray(String key) {
		String arrayLine = getProperty(key);
                if (arrayLine == null) return new int[0];
		String[] arrayValues = arrayLine.split(";");
		int[] intArray = new int[arrayValues.length];
		for (int i = 0; i < arrayValues.length; i++)
			intArray[i] = Integer.valueOf(arrayValues[i].trim());
		return (intArray);
	}

	public double[] getPropertyDoubleValueArray(String key) {
		String arrayLine = getProperty(key);
                if (arrayLine == null) return new double[0];
		String[] arrayValues = arrayLine.split(";");
		double[] doubleArray = new double[arrayValues.length];
		for (int i = 0; i < arrayValues.length; i++)
			doubleArray[i] = Double.valueOf(arrayValues[i].trim());
		return (doubleArray);
	}

	public String[] getPropertyValueArray(String key) {
                String arrayLine = getProperty(key);
                if (arrayLine == null)
			return new String[0];
		String[] arrayValues = arrayLine.split(";");
		// If we don't trim we could end up with " location" as a category, for
		// example.
		for (int i = 0; i < arrayValues.length; i++)
			arrayValues[i] = arrayValues[i].trim();
		return (arrayValues);
	}

	public Vector<String> getPropertyLowerCaseValueVector(String key) {
		Vector<String> valueSet = new Vector<String>();
                String arrayLine = getProperty(key);
                if (arrayLine != null) {
			String[] arrayValues = arrayLine.split(";");
			for (String val : arrayValues)
				if (!valueSet.contains(val))
					valueSet.add(val.trim().toLowerCase());
		}
		return (valueSet);
	}

	/**
	 * Returns a Map, whose keys are properties in this object and whose values
	 * are the Boolean interpretation of the property values.
	 * 
	 * If a property value is uninterpretable as a Boolean, it's value in the
	 * returned Map is <code>false</code>.
	 * 
	 * @return
	 */
	public Map<String, Boolean> toBooleanMap() {
		Map<String, Boolean> res = new HashMap<String, Boolean>();
		for (Map.Entry<String, String> property : entrySet()) {
			boolean val = false;
			try {
				val = Boolean.parseBoolean(property.getValue().trim());
			} catch (Exception e) {
				throw new RuntimeException("toBooleanMap() for entry " + property.toString(), e);
			}
			res.put(property.getKey(), val);
		}
		return res;
	}

	/**
	 * Returns a Map, whose keys are properties in this object and whose values
	 * are the Integer interpretation of the property values.
	 * 
	 * If a property value is uninterpretable as a Integer, it's value in the
	 * returned Map is <code>0</code>.
	 * 
	 * @return
	 */
	public Map<String, Integer> toIntegerMap() {
		Map<String, Integer> res = new HashMap<String, Integer>();
		for (Map.Entry<String, String> property : entrySet()) {
			int val = 0;
			try {
				val = Integer.parseInt(property.getValue().trim());
			} catch (Exception e) {
				throw new RuntimeException("toIntegerMap() for entry " + property.toString(), e);
			}
			res.put(property.getKey(), val);
		}
		return res;
	}

	/**
	 * @see java.util.Properties#load(InputStream)
	 * @param is
	 * @throws IOException
	 */
	public void load(InputStream is) throws IOException {
		java.util.Properties properties = new java.util.Properties();
		properties.load(is);
		load(properties);
	}

	/**
	 * @see java.util.Properties#loadFromXML(InputStream)
	 * @param is
	 * @throws IOException
	 */
	public void loadFromXML(InputStream is) throws IOException {
		java.util.Properties properties = new java.util.Properties();
		properties.loadFromXML(is);
		load(properties);
	}

	/**
	 * Adds properties from the given key-value map into this object
         *
         * Those keys that have a value of null will cause the removal of any existing value for
         * that key.
	 */
	public void load(Map<String, String> in) {
            for (final String key : in.keySet()) {
                String value = in.get(key);
                if (value == null) remove(key);
                else put(key, value);
            }
	}

	/**
	 * Adds properties from the given Properties object into this object
	 */
	public void load(final java.util.Properties in) {
		for (Map.Entry entry : in.entrySet())
			put((String) entry.getKey(), (String) entry.getValue());
	}

	/**
	 * Filters the entries in this Properties object and returns a new object
	 * containing only those entries whose keys match the given string prefix.
	 * Optionally, you may have the prefix removed from the keys in the returned
	 * Properties object.
	 * 
	 * @param prefix
	 *                the prefix string to use as a filter on existing entry
	 *                keys.
	 * @param remove_prefix
	 *                if true, strips the prefix off of all key values in the
	 *                returned Properties object.
	 * @return a new Properties object containing only those entries from this
	 *         Properties object whose keys match the given prefix.
	 */
	public Properties filterProperties(String prefix, boolean remove_prefix) {
		Properties properties = new Properties();
		for (Map.Entry<String, String> property : entrySet()) {
			String key = property.getKey();
			if (key.startsWith(prefix)) {
				if (remove_prefix)
					key = key.substring(prefix.length());
				properties.put(key, property.getValue());
			}
		}
		return properties;
	}

	/**
	 * For each key in this Properties object a prefix string is created from
	 * all leading characters before the first '.' (period) character. This
	 * prefix string is used as a new key which will reference a new Properties
	 * object containing the original key stripped of the prefix and the
	 * original value. For example, if this Properties object contains the
	 * following entries:
	 * 
	 * <pre>
	 * one = yes
	 * one.a = 1
	 * one.b = 2
	 * two.a = 3
	 * two.b = 4
	 * </pre>
	 * 
	 * then the returned Map<String,Properties> would look like this (Perl
	 * notation):
	 * 
	 * <pre>
	 * {
	 *    one =&gt; {
	 *       a =&gt; '1',
	 *       b =&gt; '2'
	 *    },
	 *    two =&gt; {
	 *       a =&gt; '3',
	 *       b =&gt; '4'
	 *    }
	 * }
	 * </pre>
	 * 
	 * @return
	 */
	public Map<String, Properties> mapProperties() {
		Map<String, Properties> properties_map = new TreeMap<String, Properties>();
		try {
			for (Map.Entry<String, String> property : entrySet()) {
				String key = property.getKey();
				int i = key.indexOf('.');
				if (i == -1) {
					Properties properties = properties_map.get(key);
					if (properties == null) {
						properties = new Properties();
						properties_map.put(key, properties);
					}
					properties.put(key, property.getValue());
					continue;
				}
				String name = key.substring(0, i);
				String property_name = key.substring(i + 1);
				Properties properties = properties_map.get(name);
				if (properties == null) {
					properties = new Properties();
					properties_map.put(name, properties);
				}
				properties.put(property_name, property.getValue());
			}
		} catch (Exception e) {
			throw new RuntimeException("mapProperties()", e);
		}
		return properties_map;
	}

	/**
	 * @return a java.util.Properties object containing equivalent key-value
	 *         entries.
	 */
	public java.util.Properties toJavaProperties() {
		java.util.Properties properties = new java.util.Properties();
		properties.putAll(this);
		return properties;
	}

    /**
     * load properties from a given file.
     *
     * This searches CLASSPATH for the properties file.  Traditionally, this looked only in a
     * hardcoded "./conf" subdirectory, but it is hoped that this change in behavior will be
     * transparent based on the assumption that all projects having a ./conf subdirectory all put
     * their log4j properties file there, in which case the ./conf subdirectory must then be in the
     * classpath.  This change was made in order to operate more standardly, and also as a bridge
     * toward offering more control of properties file location.
     *
     * If overrides for the given file name have been set by {@link setPerFileOverrides}, they will
     * be applied.
     *
     * If the override value specified for a given property is null, that will be interpreted as an
     * override that causes the given property to have no value, as if it had never been set to
     * anything.
     *
     * Sample usage: <code>
     * Properties p =
     * Properties.loadProperties(propertyFile);
     * </code>
     * 
     * @return a Properties object containing the content of the properties file
     */
    public static Properties loadProperties(String propertyFileName, boolean errorIfNotFound) {

        // New version based on org.apache.log4j.helpers.Loader (download the log4j source code to
        // see it).  Why?  Because Matlab.  The way we (currently) use Matlab is to set its dynamic
        // Java class path with javaaddpath, and then it can find our jar files and log4j can find
        // its configuration file and ideally we can find ours as well.  But, monument to logic and
        // expectable behavior that Matlab is, log4j found its configuration files while we did not.
        // Turns out the "java.class.path" property winds up having only the static path and not the
        // dynamic.  Well, of course!  Jeez.  So instead we copy log4j's approach of asking the
        // class loader to find the properties file in question, and that way this whole thing still
        // works correctly under Matlab.

        Properties p = new Properties();
        InputStream inStream =
                Properties.class.getClassLoader().getResourceAsStream(propertyFileName);
        if (inStream != null) {
            try {
                p.load(inStream);
            } catch (Exception e) {
                throw new RuntimeException("Caught exception while loading properties from "
                        + propertyFileName + ": ", e);
            }
        } else {
            if (errorIfNotFound)
                throw new RuntimeException("Unable to locate properties file " + propertyFileName
                        + " in classpath");
        }

        Map<String, String> overrides = getPerFileOverride(propertyFileName);
        if (overrides != null) p.load(overrides);

        return p;
    }

    /**
     * Same as the other loadProperties, but defaults to throwing an exception if the properties
     * file cannot be found.
     */
    public static Properties loadProperties(String propertyFileName) {
        return loadProperties(propertyFileName, true);
    }

    /**
     * Same as the other loadProperties, but then adds entires from overrideMap
     *
     * overrideMap may be null.
     *
     * If the override value specified for a given property is null, that will be interpreted as an
     * override that causes the given property to have no value, as if it had never been set to
     * anything.
     */
    public static Properties loadProperties(final String propertyFileName,
            final Map<String, String> overrideMap, boolean errorIfNotFound) {
        Properties p = loadProperties(propertyFileName, errorIfNotFound);
        if (overrideMap != null) p.load(overrideMap);
        return p;
    }

    /**
     * load properties from given a class name.
     *
     * If overrides for the given file name have been set by {@link setPerFileOverrides}, they will
     * be applied.
     * 
     * Sample usage: <code>
     * Properties p =
     * Properties.loadPropertiesFromClassName( getClass().getName() );
     * </code>
     * 
     * @return a Properties object containing the content of the properties file
     */
    public static Properties loadFromClassName(String className) {
        String propertyFileName = className + ".properties";
        return loadProperties(propertyFileName, true);
    }

    /**
     * Same as the other loadFromClassName, but then adds entires from overrideMap
     *
     * If overrides for the given file name have been set by {@link setPerFileOverrides}, they will
     * be applied.  These overrides are applied after the properties are loaded from the file but
     * before the overrides given by overrideMap.  Thus, the effects of {@link setPerFileOverride}
     * are indistinguishable from modifying the properties files themselves.
     *
     * overrideMap may be null.
     *
     * If the override value specified for a given property is null, that will be interpreted as an
     * override that causes the given property to have no value, as if it had never been set to
     * anything.
     */
    public static Properties loadFromClassName(final String className,
            final Map<String, String> overrideMap, boolean errorIfNotFound) {
        String propertyFileName = className + ".properties";
        Properties p = loadProperties(propertyFileName, errorIfNotFound);
        if (overrideMap != null) p.load(overrideMap);
        return p;
    }

    public long getPropertyLongValue(String key) {
        try {
            String value = get(key);
            if (value == null)
                throw new RuntimeException("Missing long value for property \"" + key + "\"");
            return Long.valueOf(value.trim());
        } catch (Exception e) {
            throw new RuntimeException("getPropertyLongValue(\"" + key + "\")", e);
        }
    }
    
    public long getPropertyLongValue(String key, long defaultValue) {
        try {
            String value = get(key);
            if (value == null)
                return defaultValue;
            return Long.valueOf(value.trim());
        } catch (Exception e) {
            throw new RuntimeException("getPropertyLongValue(\"" + key + "\")", e);
        }
    }

}
