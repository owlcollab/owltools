/* 
 * 
 * Copyright (c) 2010, Regents of the University of California 
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Neither the name of the Lawrence Berkeley National Lab nor the names of its contributors may be used to endorse 
 * or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package owltools.gaf.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;

/**
 * Used for reading previous or default user settings from property file and storing current user settings
 */

public class ResourceLoader {

	public static final String VERSION = "VERSION";

	private static ResourceLoader resource_loader;

	private static final Logger log = Logger.getLogger(ResourceLoader.class);

	public ResourceLoader() { //throws Exception {
	}

	public static ResourceLoader inst() {
		if (resource_loader == null) {
				resource_loader = new ResourceLoader();
		}
		return resource_loader;
	}

	public BufferedReader loadResource(String resource_name) {
		return loadResource(resource_name, false);
	}
	
	public BufferedReader loadResource(String resource_name, boolean useGzip) {
		BufferedReader reader = null;
		Class<?> c = this.getClass();
		InputStream s = c.getResourceAsStream(resource_name);
		if (s == null) {
			s = ClassLoader.getSystemResourceAsStream(resource_name);
		}
		if (useGzip) {
			try {
				s = new GZIPInputStream(s);
			} catch (IOException e) {
				log.error("Problem accessing the resource as GZIP: "+resource_name, e);
				return null;
			}
		}
		if (s != null) {
			reader = new BufferedReader(new InputStreamReader(s));
		} else if (resource_name.charAt(0) != '/') {
			reader = loadResource('/' + resource_name);
		}
		if (reader == null) {
			log.error("Unable to load resource for " + resource_name);
		}
		return reader;
	}

	public String loadVersion() {
		String v = "missing-version";
		BufferedReader reader = loadResource(VERSION);
		if (reader != null) {
			try {
				v = reader.readLine().trim();
			} catch (Exception e) {
				log.error("Unable to load version resource");
			}
		}
		return v;
	}
}
