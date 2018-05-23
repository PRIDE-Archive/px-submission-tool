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
  int errorScore = 0; // errorScore to track if there's any error(s) found during the validation
  StringBuilder errorNotes =  new StringBuilder();

  public StringBuilder getValidationReportInHTML(Submission submission, List<Report> reports) {

    StringBuilder htmlFormatted = new StringBuilder();

    htmlFormatted.append(
        "<body>\n"
                + "<div><h1>PX Submission Tool Validation Report</h1></div><br/><br/>");
    htmlFormatted.append(formatMetadata(getMetaData(submission)));
    htmlFormatted.append(formatFileValidations(reports));
    htmlFormatted.append(formatFooter());
    htmlFormatted.append(
        "" + "</body>\n"
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

    metadataSectionHTML.append("<h2>Project Metadata</h2><br/>");
    for (Map.Entry<String, Object> entry : metadata.entrySet()) {
      metadataSectionHTML.append("<p>");
      metadataSectionHTML.append("<b>");
      metadataSectionHTML.append(entry.getKey());
      metadataSectionHTML.append("</b>");
      metadataSectionHTML.append(entry.getValue());
      metadataSectionHTML.append("</p>");
    }
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

    FileValidationsSectionHTML.append("<br/><h2>File Validations</h2><br/>");
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
      if (report.getStatus().equals("OK")) {
        FileValidationsSectionHTML.append(formatFileValidationTableRaw(report));
      }else{
        errorNotes.append(report.getStatus() + "<br/>");
        logger.error(report.getStatus());
      }
    }
    FileValidationsSectionHTML.append("</tr>");
    FileValidationsSectionHTML.append("</table>");
    FileValidationsSectionHTML.append("<br/><br/>");
    if(!errorNotes.toString().equals("")){
      FileValidationsSectionHTML.append("<p class=\"incorrect\">" + errorNotes.toString() + "</p>");
      errorScore++;
      printErrorLogs(errorNotes.toString());
    }
    return FileValidationsSectionHTML;
  }

  private StringBuilder formatFileValidationTableRaw(Report report) {
    StringBuilder tableRawHTML = new StringBuilder();

    tableRawHTML.append("<td>");
    tableRawHTML.append(report.getFileName());
    tableRawHTML.append("</td>");

    // total proteins
    if(report.getTotalProteins() == 0){
      tableRawHTML.append("<td class=\"incorrect\">");
      errorNotes.append("Total number of proteins SHOULD be more than 0 <br/>");
      errorScore++;
    }else{
      tableRawHTML.append("<td>");
    }
    tableRawHTML.append(report.getTotalProteins());
    tableRawHTML.append("</td>");

    // total peptides
    if(report.getTotalPeptides() == 0){
      tableRawHTML.append("<td class=\"incorrect\">");
      errorNotes.append("Total number of peptides SHOULD be more than 0 <br/>");
      errorScore++;
    }else{
      tableRawHTML.append("<td>");
    }
    tableRawHTML.append(report.getTotalPeptides());
    tableRawHTML.append("</td>");

    // total Spectra
    if(report.getTotalSpecra() == 0){
      tableRawHTML.append("<td class=\"incorrect\">");
      errorNotes.append("Total number of spectra SHOULD be more than 0 <br/>");
      errorScore++;
    }else{
      tableRawHTML.append("<td>");
    }
    tableRawHTML.append(report.getTotalSpecra());
    tableRawHTML.append("</td>");

    tableRawHTML.append("<td>");
    tableRawHTML.append(report.getUniquePTMs().size());
    tableRawHTML.append("</td>");

    // total Spectra
    if(report.getDeltaMzPercent() > 4.0){
      tableRawHTML.append("<td class=\"warning\">");
      errorNotes.append("Warning: It is recommended to keep DeltaMzPercent below 4% <br/>");
    }else{
      tableRawHTML.append("<td>");
    }
    tableRawHTML.append(report.getDeltaMzPercent());
    tableRawHTML.append("</td>");

    // get total identified spectra
    if(report.getIdentifiedSpectra() == 0){
      tableRawHTML.append("<td class=\"incorrect\">");
      errorNotes.append("Total number of identified spectra SHOULD be more than 0 <br/>");
      errorScore++;
    }else{
      tableRawHTML.append("<td>");
    }
    tableRawHTML.append(report.getIdentifiedSpectra());
    tableRawHTML.append("</td>");

    // missing spectra
    if(report.getMissingIdSpectra() != 0){
      tableRawHTML.append("<td class=\"incorrect\">");
      errorNotes.append("Missing spectra found!<br/>");
      errorScore++;
    }else{
      tableRawHTML.append("<td>");
    }
    tableRawHTML.append(report.getMissingIdSpectra());
    tableRawHTML.append("</td>");

    return tableRawHTML;
  }

  private StringBuilder formatFooter() {
    StringBuilder footerSectionHTML = new StringBuilder();

    int newScore = 100 - (errorScore * 10);
    footerSectionHTML.append("\n<div ");
    if (newScore == 100) {
      footerSectionHTML.append("class=\"correct\"><h3><b>");
      footerSectionHTML.append("OVERALL VALIDATION SCORE: OK - ").append(newScore);
    } else {
      footerSectionHTML.append("class=\"incorrect\"><h3><b>");
      footerSectionHTML.append("OVERALL VALIDATION SCORE: FAIL - ").append(newScore);
    }
    footerSectionHTML.append("%</b></h3></div>");
    return footerSectionHTML;
  }

  private void printErrorLogs(String errorMessage){
    logger.error("++++++++++++++++++++++++++++++++++++++++\n");
    logger.error(errorMessage + "\n");
    logger.error("++++++++++++++++++++++++++++++++++++++++\n");
  }
}
