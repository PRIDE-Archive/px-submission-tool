package uk.ac.ebi.pride.gui.form;

import uk.ac.ebi.pride.App;
import uk.ac.ebi.pride.archive.submission.model.user.ContactDetail;
import uk.ac.ebi.pride.data.model.Contact;
import uk.ac.ebi.pride.data.model.Submission;
import uk.ac.ebi.pride.gui.GUIUtilities;
import uk.ac.ebi.pride.gui.blocker.DefaultGUIBlocker;
import uk.ac.ebi.pride.gui.blocker.GUIBlocker;
import uk.ac.ebi.pride.gui.data.SubmissionRecord;
import uk.ac.ebi.pride.gui.form.comp.ContextAwareNavigationPanelDescriptor;
import uk.ac.ebi.pride.gui.navigation.Navigator;
import uk.ac.ebi.pride.gui.task.GetPrideUserDetailTask;
import uk.ac.ebi.pride.gui.task.Task;
import uk.ac.ebi.pride.gui.task.TaskEvent;
import uk.ac.ebi.pride.gui.task.TaskListener;
import uk.ac.ebi.pride.gui.util.ValidationState;

import javax.help.HelpBroker;
import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author Rui Wang
 * @version $Id$
 *          <p/>
 */
public class PrideLoginDescriptor extends ContextAwareNavigationPanelDescriptor implements TaskListener<ContactDetail, String> {

    public PrideLoginDescriptor(String id, String title, String desc) {
        super(id, title, desc, new PrideLoginForm());
    }

    /**
     * Method to be performed to show help document
     * Override this method to add help functionality
     */
    @Override
    public void getHelp() {
        HelpBroker hb = appContext.getMainHelpBroker();
        hb.showID("help.login", "javax.help.SecondaryWindow", "main");
    }

    @Override
    public void beforeDisplayingPanel() {
        Submission submission = appContext.getSubmissionRecord().getSubmission();
        Contact contact = submission.getProjectMetaData().getSubmitterContact();

        if (contact != null) {
            // set previous username and password
            setExistingSubmitter(contact);
        }

        firePropertyChange(BEFORE_DISPLAY_PANEL_PROPERTY, false, true);
    }

    @Override
    public void displayingPanel() {
        Navigator navigator = ((App) App.getInstance()).getNavigator();
        JButton nextButton = navigator.getNextButton();

        nextButton.setText(appContext.getProperty("pride.login.button.title"));
    }

    private void setExistingSubmitter(Contact contact) {
        PrideLoginForm form = (PrideLoginForm) getNavigationPanel();
        form.setUserName(contact.getUserName());
        form.setPassword(contact.getPassword());
        form.setSubmitterName(contact.getName());
        form.setAffiliation(contact.getAffiliation());
        form.setEmail(contact.getEmail());
    }

    @Override
    public void beforeHidingForNextPanel() {
        // validate the content in the table
        PrideLoginForm form = (PrideLoginForm) getNavigationPanel();
        ValidationState state = form.doValidation();
        if (!ValidationState.ERROR.equals(state)) {
            getPrideUserDetail(form);
        } else {
            // notify validation error
            firePropertyChange(BEFORE_HIDING_FOR_NEXT_PANEL_PROPERTY, true, false);
        }
    }

    private void getPrideUserDetail(PrideLoginForm form) {
        String userName = form.getUserName();
        char[] password = form.getPassword();

        // record user name and password
        SubmissionRecord submissionRecord = appContext.getSubmissionRecord();
        submissionRecord.setUserName(userName);
        submissionRecord.setPassword(new String(password));

        // launch a new task for login
        Task task = new GetPrideUserDetailTask(userName, password);
        task.addTaskListener(this);
        task.setGUIBlocker(new DefaultGUIBlocker(task, GUIBlocker.Scope.NONE, null));
        App.getInstance().getDesktopContext().addTask(task);
    }

    @Override
    public void beforeHidingForPreviousPanel() {
        //hide any visible warning balloon tip
        PrideLoginForm form = (PrideLoginForm) getNavigationPanel();
        form.hideWarnings();
        saveFormContent();

        firePropertyChange(BEFORE_HIDING_FOR_PREVIOUS_PANEL_PROPERTY, false, true);
    }

    @Override
    public void succeed(TaskEvent<ContactDetail> event) {
        ContactDetail details = event.getValue();

        if (details != null) {
            // set name and affiliation
            updateFormContent(details);

            saveFormContent();

            // hide warnings
            PrideLoginForm form = (PrideLoginForm) getNavigationPanel();
            form.hideWarnings();

            // notify success
            firePropertyChange(BEFORE_HIDING_FOR_NEXT_PANEL_PROPERTY, false, true);
        }
    }

    /**
     * Save the content from the form to AppContext
     */
    private void saveFormContent() {
        PrideLoginForm form = (PrideLoginForm) getNavigationPanel();
        Submission submission = appContext.getSubmissionRecord().getSubmission();

        Contact contact = submission.getProjectMetaData().getSubmitterContact();
        if (contact == null) {
            contact = new Contact();
            submission.getProjectMetaData().setSubmitterContact(contact);
        }

        contact.setUserName(form.getUserName());
        contact.setPassword(form.getPassword());
        contact.setName(form.getSubmitterName());
        contact.setAffiliation(form.getAffiliation());
        contact.setEmail(form.getEmail());
    }

    private void updateFormContent(ContactDetail details) {
        PrideLoginForm form = (PrideLoginForm) getNavigationPanel();

        String submitterName = details.getFirstName() + " " + details.getLastName();
        String affiliation = details.getAffiliation();
        String email = details.getEmail();

        form.setSubmitterName(submitterName);
        form.setAffiliation(affiliation);
        form.setEmail(email);
    }

    @Override
    public void started(TaskEvent<Void> event) {
        //set the in-progress icon
        Navigator navigator = app.getNavigator();
        JButton nextButton = navigator.getNextButton();

        Icon newIcon = GUIUtilities.loadIcon(appContext.getProperty("pride.login.button.loading.small.icon"));
        nextButton.setIcon(newIcon);
    }

    @Override
    public void process(TaskEvent<List<String>> event) {
        Runnable eventDispatcher = new Runnable() {
            public void run() {
                // show warning dialog
                JOptionPane.showConfirmDialog(app.getMainFrame(),
                        appContext.getProperty("pride.login.error.message"),
                        appContext.getProperty("pride.login.error.title"),
                        JOptionPane.CLOSED_OPTION, JOptionPane.WARNING_MESSAGE);
            }
        };
        EventQueue.invokeLater(eventDispatcher);
    }

    @Override
    public void finished(TaskEvent<Void> event) {
        // replace the in-process icon with next button
        Navigator navigator = app.getNavigator();
        JButton nextButton = navigator.getNextButton();

        Icon newIcon = GUIUtilities.loadIcon(appContext.getProperty("next.button.small.icon"));
        nextButton.setIcon(newIcon);
    }

    @Override
    public void failed(TaskEvent<Throwable> event) {
    }

    @Override
    public void cancelled(TaskEvent<Void> event) {
    }

    @Override
    public void interrupted(TaskEvent<InterruptedException> iex) {
    }

    @Override
    public void progress(TaskEvent<Integer> progress) {
    }
}
