/*
 * Copyright (c) 2014 to present, doctainer gmbh
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.maushake.bootstrap;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 
 * (C) doctainer 2001-2009<br/>
 * <br/>
 * 
 * PoM io
 * 
 * <h2>LoadProperties</h2> <h3>Desciption</h3>
 * <p>
 * Provide mechanisms to read properties
 * </p>
 * 
 * 
 * 
 * <h4>change log</h4>
 * <ul>
 * <li>jm912-bdk-central
 * <li>LoadProperties created 26.11.2009
 * </ul>
 */
public class LoadProperties {
	/**
	 * Note, this method is borrowed from LoadProperties where it is maintained
	 * 
	 * @param file
	 * @return the properties
	 * @throws IOException
	 * 
	 *             PoM org.maushake.io.files.LoadProperties, vsn 1.0
	 * 
	 *             <pre>
	 * jm711getProperties
	 * </pre>
	 */
	public static Properties getPropertiesFromFile(String file)
			throws IOException {
		return getPropertiesFromFile(new File(file));
	}

	/**
	 * Note, this method is borrowed from LoadProperties where it is maintained
	 * 
	 * @param file
	 * @return the properties
	 * @throws IOException
	 * 
	 *             PoM org.maushake.io.files.LoadProperties, vsn 1.0
	 * 
	 *             <pre>
	 * jm711getProperties
	 * </pre>
	 */
	public static Properties getPropertiesFromFile(File file)
			throws IOException {
		Properties props = new Properties();
		BufferedInputStream bIS = new BufferedInputStream(new FileInputStream(
				file));
		try {
			props.load(bIS);
			return props;
		} finally {
			try {
				bIS.close();
			} catch (IOException ignored) {
			}
		} // finally
	}

	// jm912-bdk-central
	public static Properties getPropertiesFromUserHome(String file)
			throws IOException {
		String f = System.getProperty("user.home") + '/' + file;
		return getPropertiesFromFile(f);
	}

	public static Properties getPropertiesFromClasspath(String path)
			throws IOException {
		InputStream in = LoadProperties.class.getResourceAsStream(path);
		if (in == null)
			throw new FileNotFoundException(path);
		Properties props = new Properties();
		try {
			props.load(in);
			return props;
		} finally {
			try {
				in.close();
			} catch (IOException ignored) {
			}
		}
	}

	/**
	 * 
	 * like getPropertiesFromFile returns null if it can't access the file.
	 * prints some information to stderr if it ecounters the java malformed
	 * encoding bug.
	 * 
	 * PoM org.maushake.io.files.LoadProperties, vsn 1.0
	 * 
	 * @param possibleProps
	 *            - path to property file to read
	 * @return the properties read of null if not successful
	 */
	public static Properties safelyGetPropertiesFromFile(String possibleProps) {
		try {
			return getPropertiesFromFile(possibleProps);
		} catch (IOException e) {
			return null;
		} catch (IllegalArgumentException ill) {
			System.err.println("There is a problem due to the content of: "
					+ possibleProps.replace('\\', '/'));
			if ("Malformed \\uxxxx encoding.".equals(ill.getMessage())) {
				System.err.println("Please avoid backslashes (\\).");
			}
			return null;
		}
	}
}
