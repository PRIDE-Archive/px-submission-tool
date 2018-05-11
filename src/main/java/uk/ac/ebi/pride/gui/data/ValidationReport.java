package uk.ac.ebi.pride.gui.data;

import uk.ac.ebi.pride.data.model.CvParam;
import uk.ac.ebi.pride.data.model.ProjectMetaData;
import uk.ac.ebi.pride.data.model.Submission;

/** @author Suresh Hewapathirana */
public class ValidationReport {

  private Submission submission;
  private ProjectMetaData projectMetaData;

  public ValidationReport(Submission submission) {
    this.submission = submission;
    this.projectMetaData = submission.getProjectMetaData();
  }

  public int getNumberOfFiles() {
    return (submission.getDataFiles() != null) ? submission.getDataFiles().size() : 0;
  }

  public String getProjectTitle() {
    return projectMetaData.getProjectTitle();
  }

  public String getProjectDescription() {
    return projectMetaData.getProjectDescription();
  }

  public String getSampleProcessingProtocol() {
    return projectMetaData.getSampleProcessingProtocol();
  }

  public String getDataProcessingProtocol() {
    return projectMetaData.getDataProcessingProtocol();
  }

  public String getOtherOmicsLinks() {
    return (projectMetaData.getOtherOmicsLink() != null )?projectMetaData.getOtherOmicsLink(): "";
  }

  public String getSubmissionType() {
    return projectMetaData.getSubmissionType().toString();
  }

  public String getProjectTag() {
    int count = 1;
    String formattedProjectTag = "";
    for (String tag : projectMetaData.getProjectTags()) {
      formattedProjectTag += "(" + count + ")" + tag;
    }
    return formattedProjectTag;
  }

  public String getSpecies() {
    int count = 1;
    String formattedSpecies = "";
    for (CvParam species : projectMetaData.getSpecies()) {
      formattedSpecies += "(" + count + ")" + species.toString();
    }
    return formattedSpecies;
  }

  public String getTissues() {
    int count = 1;
    String formattedTissues = "";
    for (CvParam tissue : projectMetaData.getTissues()) {
      formattedTissues += "(" + count + ")" + tissue.toString();
    }
    return formattedTissues;
  }

  public String getModification() {
    int count = 1;
    String formattedModification = "";
    for (CvParam modification : projectMetaData.getModifications()) {
      formattedModification += "(" + count + ")" + modification.toString();
    }
    return formattedModification;
  }

  public String getInstruments() {
    int count = 1;
    String formattedInstruments = "";
    for (CvParam instruments : projectMetaData.getInstruments()) {
      formattedInstruments += "(" + count + ")" + instruments.toString();
    }
    return formattedInstruments;
  }

  public String getLabHead() {
    String labHeads = "";
    uk.ac.ebi.pride.data.model.Contact labHeadContact = projectMetaData.getLabHeadContact();
    if (labHeadContact.getName() != null) {
      labHeads = labHeadContact.getName();
    }
    if (labHeadContact.getEmail() != null) {
      labHeads += "(" + labHeadContact.getEmail() + ")";
    }
    return labHeads;
  }

  public String getSubmitter() {
    return projectMetaData.getSubmitterContact().toString();
  }

  public String getResubmissionPxAccession() {
    return (projectMetaData.getResubmissionPxAccession() != null )?projectMetaData.getResubmissionPxAccession(): "";
  }

  public String getReanalysisAccessions() {
    int count = 1;
    String formattedReanalysisAccessions = "";
    for (String reanalysisAccession : projectMetaData.getReanalysisAccessions()) {
      formattedReanalysisAccessions += "(" + count + ")" + reanalysisAccession;
    }
    return formattedReanalysisAccessions;
  }

  public String getPubmedIds() {
    int count = 1;
    String formattedPubmedIds = "";
    for (String pubmedId : projectMetaData.getPubmedIds()) {
      formattedPubmedIds += "(" + count + ")" + pubmedId;
    }
    return formattedPubmedIds;
  }
}
