package fr.myitworld;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Goal which touches a timestamp file.
 *
 * @author imadsalki
 * @goal touch
 * @phase process-sources
 */
@Mojo(name = "CoverageInfos", defaultPhase = LifecyclePhase.TEST)
public class MyMojo extends AbstractMojo {
    // Logger
    Logger logger = LoggerFactory.getLogger(MyMojo.class);

    /**
     * XML document absolute path
     */
    @Parameter(defaultValue = "")
    private File coverageResult;

    /**
     * MOJO Execution
     *
     * @throws MojoExecutionException
     */
    public void execute() throws MojoExecutionException {

        boolean exists = coverageResult.exists();

        if (exists) {
            logger.info(" -------------------------");
            logger.info("|   Coverage Results     |");
            logger.info(" -------------------------");

            Document document;
            try {
                document = readAndReformatXml(coverageResult.toPath());
            } catch (IOException | ParserConfigurationException | SAXException e) {
                e.printStackTrace();
                return;
            }

            // Get the root element
            var root = document.getDocumentElement();

            // Display Information
            displayInformation(root);

        } else {
            logger.info("No coverage tests found !");
        }
    }

    /**
     * Read and and reformat XML file
     *
     * @return Document
     * @throws IOException
     * @throws ParserConfigurationException
     */
    private Document readAndReformatXml(Path path) throws IOException, ParserConfigurationException, SAXException {
        Charset charset = StandardCharsets.UTF_8;
        String content = Files.readString(path, charset);
        content = content.replaceAll("(?i)<!DOCTYPE[^<>]*(?:<!ENTITY[^<>]*>[^<>]*)?>", "");

        //Get Document Builder
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        //Build Document
        Document document = builder.parse(new InputSource(new StringReader(content)));

        //Normalize the XML Structure
        document.getDocumentElement().normalize();

        return document;
    }

    /**
     * Permet D'afficher les informations
     */
    private void displayInformation(Element root) {
        Node child = root.getFirstChild();
        logger.info("-----------------------------------------------------");
        while (child != null) {

            if ("counter".equals(child.getNodeName())) {
                logNodeInformation(child);
            }

            if (child.getNextSibling() == null) {
                break;
            }

            child = child.getNextSibling();
        }
        logger.info("-----------------------------------------------------");
    }

    /**
     * Permet d'afficher les informations du Node
     *
     * @param node
     */
    private void logNodeInformation(Node node) {
        NamedNodeMap attributes = node.getAttributes();

        var nbMissed = Integer.parseInt(attributes.getNamedItem("missed").getNodeValue());
        var nbCovered = Integer.parseInt(attributes.getNamedItem("covered").getNodeValue());

        var type = attributes.getNamedItem("type").getNodeValue().toLowerCase();
        var typeFormatted = String.format("%-13s", type.substring(0, 1).toUpperCase() + type.substring(1));
        var missed = String.format("%-13s", "Missed: " + nbMissed);
        var covered = String.format("%-13s", "Covered: " + nbCovered);
        var percentage = nbCovered * 100 / nbMissed;
        var percentageFormatted = "instruction".equals(type) ? "Total." + percentage : String.valueOf(percentage);

        logger.info("- {} - {} - {} ({}%)", typeFormatted, missed, covered, percentageFormatted);
    }
}
