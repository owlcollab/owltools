package owltools.ontologyrelease;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.FileUtils;

@Deprecated
public class BuildVersionId extends Task {

    /**
     * The name of the property in which the build number is stored.
     */
    private static final String DEFAULT_PROPERTY_NAME = "oborelease.versionid";

    /** The default filename to use if no file specified.  */
    private static final String DEFAULT_FILENAME = "VERSION-INFO";

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    /** The File in which the build number is stored.  */
    private File myFile;


    /**
     * The file in which the build number is stored. Defaults to
     * "build.number" if not specified.
     *
     * @param file the file in which build number is stored.
     */
    public void setFile(final File file) {
        myFile = file;
    }


    /**
     * Run task.
     *
     * @exception BuildException if an error occurs
     */
    public void execute()
         throws BuildException {
        File savedFile = myFile; // may be altered in validate

        validate();

        final Properties properties = loadProperties();
    /*    double versionid = getBuildNumber(properties);
        
        versionid = versionid +  0.01;
        DecimalFormat df = new DecimalFormat("#.##");        
        
        String versionidString = df.format(versionid);
      */

        SimpleDateFormat dtFormat = new SimpleDateFormat("yyyy-MM-dd");
        String versionidString = dtFormat.format(Calendar.getInstance().getTime());
        
        properties.put(DEFAULT_PROPERTY_NAME,
            versionidString);

        // Write the properties file back out
        FileOutputStream output = null;

        try {
            output = new FileOutputStream(myFile);

            final String header = "Version Id for Obo Ontology Release Manager. Do not edit!";

            properties.store(output, header);
        } catch (final IOException ioe) {
            final String message = "Error while writing " + myFile;

            throw new BuildException(message, ioe);
        } finally {
            if (null != output) {
                try {
                    output.close();
                } catch (final IOException ioe) {
                    log("error closing output stream " + ioe, Project.MSG_ERR);
                }
            }
            myFile = savedFile;
        }

        //Finally set the property
        getProject().setNewProperty(DEFAULT_PROPERTY_NAME,
            versionidString);
    }


    /**
     * Utility method to retrieve build number from properties object.
     *
     * @param properties the properties to retrieve build number from
     * @return the build number or if no number in properties object
     * @throws BuildException if build.number property is not an integer
     */
    
    /*
    private double getBuildNumber(final Properties properties)
         throws BuildException {
        final String buildNumber =
            properties.getProperty(DEFAULT_PROPERTY_NAME, "0.00").trim();

        // Try parsing the line into an double.
        try {
            return Double.parseDouble(buildNumber);
        } catch (final NumberFormatException nfe) {
            final String message =
                myFile + " contains a non numberic build number: " + buildNumber;
            throw new BuildException(message, nfe);
        }
    }*/


    /**
     * Utility method to load properties from file.
     *
     * @return the loaded properties
     * @throws BuildException
     */
    private Properties loadProperties()
         throws BuildException {
        FileInputStream input = null;

        try {
            final Properties properties = new Properties();

            input = new FileInputStream(myFile);
            properties.load(input);
            return properties;
        } catch (final IOException ioe) {
            throw new BuildException(ioe);
        } finally {
            if (null != input) {
                try {
                    input.close();
                } catch (final IOException ioe) {
                    log("error closing input stream " + ioe, Project.MSG_ERR);
                }
            }
        }
    }


    /**
     * Validate that the task parameters are valid.
     *
     * @throws BuildException if parameters are invalid
     */
    private void validate()
         throws BuildException {
        if (null == myFile) {
            myFile = FILE_UTILS.resolveFile(getProject().getBaseDir(), DEFAULT_FILENAME);
        }

        if (!myFile.exists()) {
            try {
                FILE_UTILS.createNewFile(myFile);
            } catch (final IOException ioe) {
                final String message =
                    myFile + " doesn't exist and new file can't be created.";
                throw new BuildException(message, ioe);
            }
        }

        if (!myFile.canRead()) {
            final String message = "Unable to read from " + myFile + ".";
            throw new BuildException(message);
        }

        if (!myFile.canWrite()) {
            final String message = "Unable to write to " + myFile + ".";
            throw new BuildException(message);
        }
    }
	
}
