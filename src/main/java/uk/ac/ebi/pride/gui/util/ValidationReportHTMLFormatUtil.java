package uk.ac.ebi.pride.gui.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.data.model.Submission;
import uk.ac.ebi.pride.gui.data.ValidationReport;
import uk.ac.ebi.pride.utilities.data.controller.tools.utils.Report;
import java.util.*;

/**
 * This call constructs the validation report in HTML format. It would be nice to have a HTML template engine to render
 * in HTML, however it is not easy to find a compatible library for Swing. Therefore, I used StringBuilder approach.
 *
 * @author Suresh Hewapathirana
 * */
public class ValidationReportHTMLFormatUtil {

  private static final Logger logger = LoggerFactory.getLogger(ValidationReportHTMLFormatUtil.class);
  int score = 0; // score to track if there's any error(s) found during the validation

  public StringBuilder getValidationReportInHTML(Submission submission, List<Report> reports) {

    StringBuilder htmlFormatted = new StringBuilder();

    htmlFormatted.append(
        //          "<!DOCTYPE html>\n"
        //              + "<html lang=\"en\">\n"
        //              + "<head>\n"
        //              + "<title>"
        //              + "PX Submission Tool Validation Report"
        //              + "</title>\n"
        //              + "<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\"
        // />\n"
        //              + "</head>"
        "<body>\n"
                + "<div><h1>PX Submission Tool Validation Report</h1></div><br/><br/>");
    htmlFormatted.append(formatMetadata(getMetaData(submission)));
    htmlFormatted.append(formatFileValidations(reports));
    htmlFormatted.append(formatFooter());
    htmlFormatted.append(
        "" + "</body>\n"
        //              +"</html>"
        );
    logger.info("Generated html report: " + htmlFormatted.toString());
    return htmlFormatted;
  }

  private Map<String, Object> getMetaData(Submission submission) {

    ValidationReport validationReport = new ValidationReport(submission);
    Map<String, Object> metadata = new LinkedHashMap<>();

    metadata.put("Number of files : ", validationReport.getNumberOfFiles());
    metadata.put("Project title : ", validationReport.getProjectTitle());
    metadata.put("Project description : ", validationReport.getProjectDescription());
    metadata.put("Sample processing protocol : ", validationReport.getSampleProcessingProtocol());
    metadata.put("Data processing protocol : ", validationReport.getDataProcessingProtocol());
    metadata.put("Other omics link : ", validationReport.getOtherOmicsLinks());
    metadata.put("Submission type : ", validationReport.getSubmissionType());
    metadata.put("Project tag : ", validationReport.getProjectTag());
    metadata.put("Species : ", validationReport.getSpecies());
    metadata.put("Tissue : ", validationReport.getTissues());
    metadata.put("Modification : ", validationReport.getModification());
    metadata.put("Instrument : ", validationReport.getInstruments());
    metadata.put("Lab head : ", validationReport.getLabHead());
    metadata.put("Submitter : ", validationReport.getSubmitter());
    metadata.put("Resubmission accession : ", validationReport.getResubmissionPxAccession());
    metadata.put("Reanalysis accession : ", validationReport.getReanalysisAccessions());
    metadata.put("Pubmed id : ", validationReport.getPubmedIds());

    return metadata;
  }

  private StringBuilder formatMetadata(Map<String, Object> metadata) {

    StringBuilder metadataSectionHTML = new StringBuilder();

    metadataSectionHTML.append("<h4>Project Metadata</h4><br/><br/>");
    for (Map.Entry<String, Object> entry : metadata.entrySet()) {
      metadataSectionHTML.append("<p>");
      metadataSectionHTML.append("<b>");
      metadataSectionHTML.append(entry.getKey());
      metadataSectionHTML.append("</b>");
      metadataSectionHTML.append(entry.getValue());
      metadataSectionHTML.append("</p>");
    }
    metadataSectionHTML.append("<br/><br/>");
    return metadataSectionHTML;
  }

  private StringBuilder formatFileValidations(List<Report> reports) {

    List<String> tableHeaders =
        Arrays.asList(
            "File name",
            "#Proteins",
            "#Peptides",
            "#Spectra",
            "#Unique PTMs",
            "#Delta m/z %",
            "#Identified spectra",
            "#Missing identified spectra");
    StringBuilder FileValidationsSectionHTML = new StringBuilder();

    FileValidationsSectionHTML.append("<h4>File Validations</h4><br/><br/>");
    FileValidationsSectionHTML.append("<table>");
    // header
    FileValidationsSectionHTML.append("<tr>");
    for (String tableHeader : tableHeaders) {
      FileValidationsSectionHTML.append("<th>");
      FileValidationsSectionHTML.append(tableHeader);
      FileValidationsSectionHTML.append("</th>");
    }
    FileValidationsSectionHTML.append("</tr>");
    // Row(s)
    FileValidationsSectionHTML.append("<tr>");
    for (Report report : reports) {
      FileValidationsSectionHTML.append(formatRaw(report));
    }
    FileValidationsSectionHTML.append("</tr>");
    FileValidationsSectionHTML.append("</table>");
    FileValidationsSectionHTML.append("<br/><br/>");
    return FileValidationsSectionHTML;
  }

  private StringBuilder formatFooter() {
    StringBuilder footerSectionHTML = new StringBuilder();

//    <div class="incorrect"><h3>OVERALL VALIDATION SCORE: OK - 100%</h3></div>

    int newScore = 100 - (score * 10);
    footerSectionHTML.append("\n<div ");
    if (newScore == 100) {
      footerSectionHTML.append("class=\"correct\"><h3>");
      footerSectionHTML.append("OVERALL VALIDATION SCORE: OK - ").append(newScore);
    } else {
      footerSectionHTML.append("class=\"incorrect\"><h3>");
      footerSectionHTML.append("OVERALL VALIDATION SCORE: FAIL - ").append(newScore);
    }
    footerSectionHTML.append("0%</h3></div>");
    return footerSectionHTML;
  }

  private StringBuilder formatRaw(Report report) {
    StringBuilder tableRawHTML = new StringBuilder();

    tableRawHTML.append("<td>");
    tableRawHTML.append(report.getFileName());
    tableRawHTML.append("</td>");

    tableRawHTML.append("<td>");
    tableRawHTML.append(report.getTotalProteins());
    tableRawHTML.append("</td>");

    tableRawHTML.append("<td>");
    tableRawHTML.append(report.getTotalPeptides());
    tableRawHTML.append("</td>");

    tableRawHTML.append("<td>");
    tableRawHTML.append(report.getTotalSpecra());
    tableRawHTML.append("</td>");

    tableRawHTML.append("<td>");
    tableRawHTML.append(report.getUniquePTMs().size());
    tableRawHTML.append("</td>");

    tableRawHTML.append("<td>");
    tableRawHTML.append(report.getDeltaMzPercent());
    tableRawHTML.append("</td>");

    tableRawHTML.append("<td>");
    tableRawHTML.append(report.getIdentifiedSpectra());
    tableRawHTML.append("</td>");

    tableRawHTML.append("<td>");
    tableRawHTML.append(report.getMissingIdSpectra());
    tableRawHTML.append("</td>");

    return tableRawHTML;
  }
}
