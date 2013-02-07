package owltools.ontologyrelease;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import owltools.ontologyrelease.OortConfiguration;

/**
 * Tests for {@link OortConfiguration}.
 */
public class OortConfigurationTest {

	private static File tempFile = null;
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		tempFile = File.createTempFile("OortConfigurationTest-", ".properties.test");
	}
	
	public static void afterClass() throws Exception {
		FileUtils.deleteQuietly(tempFile);
	}
	
	@Test
	public void testParseOortParamters() throws Exception {
		
		OortConfiguration oortConfig = new OortConfiguration();
		String[] args = new String[] {
			"--ignoreLock", 
			"--skip-release-folder",
			"--skip-format", "owx", 
			"--outdir", ".", 
			"--allow-overwrite", 
			"--asserted", 
			"--simple", 
			"--reasoner", "hermit", 
			"editors/gene_ontology_write.obo", 
			"extensions/x-disjoint.owl"
		};
		OboOntologyReleaseRunner.parseOortCommandLineOptions(args, oortConfig);
		
		Vector<String> paths = oortConfig.getPaths();
		assertArrayEquals(new String[]{
				"editors/gene_ontology_write.obo", 
				"extensions/x-disjoint.owl"}, 
		paths.toArray());
		
		assertTrue(oortConfig.isIgnoreLockFile());
		assertEquals("hermit", oortConfig.getReasonerName());
	}
	
	/**
	 * Tests for {@link OortConfiguration#escape(String, char)}
	 */
	@Test
	public void testEscape() {
		assertEquals("", OortConfiguration.escape("", ','));
		assertEquals("a", OortConfiguration.escape("a", ','));
		assertEquals("\\\\", OortConfiguration.escape("\\", ',').toString());
		assertEquals("aa\\,bb", OortConfiguration.escape("aa,bb", ',').toString());
	}

	/**
	 * Tests for {@link OortConfiguration#unescape(String)}
	 */
	@Test
	public void testUnescape() {
		assertEquals("", OortConfiguration.unescape(""));
		assertEquals("a", OortConfiguration.unescape("a"));
		assertEquals("aa,bb", OortConfiguration.unescape("aa\\,bb"));
		assertEquals("\\", OortConfiguration.unescape("\\\\"));
	}
	
	/**
	 * Tests for {@link OortConfiguration#addValues(String, java.util.Collection)}
	 */
	@Test
	public void testSplitList() {
		assertTrue(split("").isEmpty());
		assertTrue(split(",").isEmpty());
		assertTrue(split(",,,").isEmpty());
		assertArrayEquals(new String[]{"a"}, splitArray("a"));
		assertArrayEquals(new String[]{"a"}, splitArray("a,"));
		assertArrayEquals(new String[]{"a","b"}, splitArray("a,b"));
		assertArrayEquals(new String[]{"a","b"}, splitArray("a,,,b"));
		assertArrayEquals(new String[]{"a,b"}, splitArray("a\\,b"));
		assertArrayEquals(new String[]{"aaa","bbbb","cc"}, splitArray("aaa,bbbb,cc"));
	}
	
	private List<String> split(String s) {
		ArrayList<String> values = new ArrayList<String>();
		OortConfiguration.addValues(s, values);
		return values;
	}
	
	private String[] splitArray(String s) {
		return split(s).toArray(new String[0]);
	}

	/**
	 * Tests for {@link OortConfiguration#writeConfig(File, OortConfiguration)} 
	 * and {@link OortConfiguration#readConfig(File)}.
	 * 
	 * @throws IOException 
	 */
	@Test
	public void testReadWriteConfig() throws IOException {
		OortConfiguration configuration = new OortConfiguration();
		configuration.addPath("/path/path1");
		
		OortConfiguration.writeConfig(tempFile, configuration);
		
		OortConfiguration configuration2 = OortConfiguration.readConfig(tempFile);
		assertArrayEquals(new String[]{"/path/path1"}, configuration2.getPaths().toArray());
	}
}
