package org.tmatesoft.svn.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.wc.SVNFileListUtil;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNCommitClient;
import org.tmatesoft.svn.core.wc.SVNCopyClient;
import org.tmatesoft.svn.core.wc.SVNCopySource;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCClient;

public class WorkingCopy {

    private final TestOptions testOptions;
    private final File workingCopyDirectory;

    private SVNClientManager clientManager;
    private long currentRevision;

    private PrintWriter logger;
    private SVNURL repositoryUrl;

    public WorkingCopy(String testName, File workingCopyDirectory) {
        this(TestOptions.getInstance(), workingCopyDirectory);
    }

    public WorkingCopy(TestOptions testOptions, File workingCopyDirectory) {
        this.testOptions = testOptions;
        this.workingCopyDirectory = workingCopyDirectory;
        this.currentRevision = -1;
    }

    public long checkoutLatestRevision(SVNURL repositoryUrl) throws SVNException {
        beforeOperation();

        log("Checking out " + repositoryUrl);

        final SVNUpdateClient updateClient = getClientManager().getUpdateClient();

        this.repositoryUrl = repositoryUrl;

        currentRevision = updateClient.doCheckout(repositoryUrl,
                getWorkingCopyDirectory(),
                SVNRevision.HEAD,
                SVNRevision.HEAD,
                SVNDepth.INFINITY,
                true);

        log("Checked out " + repositoryUrl);

        afterOperation();

        return currentRevision;
    }

    public void updateToRevision(long revision) throws SVNException {
        beforeOperation();

        log("Updating to revision " + revision);

        final SVNUpdateClient updateClient = getClientManager().getUpdateClient();

        currentRevision = updateClient.doUpdate(getWorkingCopyDirectory(),
                SVNRevision.create(revision),
                SVNDepth.INFINITY,
                true,
                true);

        log("Updated to revision " + currentRevision);

        afterOperation();
    }

    public File findAnyDirectory() {
        final File workingCopyDirectory = getWorkingCopyDirectory();
        final File[] files = SVNFileListUtil.listFiles(workingCopyDirectory);
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() && !file.getName().equals(SVNFileUtil.getAdminDirectoryName())) {
                    return file;
                }
            }
        }

        throw new RuntimeException("The repository contains no directories, run the tests with another repository");
    }

    public File findAnotherDirectory(File directory) {
        final File workingCopyDirectory = getWorkingCopyDirectory();
        final File[] files = SVNFileListUtil.listFiles(workingCopyDirectory);
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() && !file.getName().equals(SVNFileUtil.getAdminDirectoryName()) && !file.getAbsolutePath().equals(directory.getAbsolutePath())) {
                    return file;
                }
            }
        }

        throw new RuntimeException("The repository root should contain at least two directories, please run the tests with another repository");
    }

    public void copyAsChild(File directory, File anotherDirectory) throws SVNException {
        beforeOperation();

        log("Copying " + directory + " as a child of " + anotherDirectory);

        final SVNCopyClient copyClient = getClientManager().getCopyClient();

        copyClient.doCopy(new SVNCopySource[]{
                new SVNCopySource(SVNRevision.WORKING, SVNRevision.WORKING, directory)},
                anotherDirectory,
                false, false, false);

        log("Copyied " + directory + " as a child of " + anotherDirectory);

        afterOperation();
    }

    public long commit(String commitMessage) throws SVNException {
        beforeOperation();

        log("Committing ");

        final SVNCommitClient commitClient = getClientManager().getCommitClient();

        final SVNCommitInfo commitInfo = commitClient.doCommit(new File[]{getWorkingCopyDirectory()},
                false,
                commitMessage,
                null, null,
                false, true,
                SVNDepth.INFINITY);

        log("Committed revision " + commitInfo.getNewRevision());

        afterOperation();

        return commitInfo.getNewRevision();
    }

    public void add(File file) throws SVNException {
        beforeOperation();

        log("Adding " + file);

        final SVNWCClient wcClient = getClientManager().getWCClient();

        wcClient.doAdd(file, false, false, false, SVNDepth.INFINITY, true, true, true);

        log("Added " + file);

        afterOperation();
    }

    public void revert() throws SVNException {
        beforeOperation();

        log("Reverting working copy");

        final SVNWCClient wcClient = getClientManager().getWCClient();

        wcClient.doRevert(new File[]{getWorkingCopyDirectory()}, SVNDepth.INFINITY, null);

        log("Reverted working copy");

        afterOperation();
    }

    public List<File> getChildren() {
        final List<File> childrenList = new ArrayList<File>();

        final File[] children = SVNFileListUtil.listFiles(getWorkingCopyDirectory());
        if (children != null) {
            for (File child : children) {
                if (!child.getName().equals(SVNFileUtil.getAdminDirectoryName())) {
                    childrenList.add(child);
                }
            }
        }
        return childrenList;
    }

    public void setProperty(File file, String propertyName, SVNPropertyValue propertyValue) throws SVNException {
        beforeOperation();

        log("Setting property " + propertyName + " on " + file);

        final SVNWCClient wcClient = getClientManager().getWCClient();

        wcClient.doSetProperty(file, propertyName, propertyValue, true, SVNDepth.INFINITY, null, null);

        log("Set property " + propertyName + " on " + file);

        afterOperation();
    }

    public void delete(File file) throws SVNException {
        beforeOperation();

        log("Deleting " + file);

        final SVNWCClient wcClient = getClientManager().getWCClient();

        wcClient.doDelete(file, true, false);

        log("Deleted " + file);

        afterOperation();
    }

    public long getCurrentRevision() {
        return currentRevision;
    }

    public SVNURL getRepositoryUrl() {
        return repositoryUrl;
    }

    private void backupWcDbFile() throws SVNException {
        final File wcDbFile = getWCDbFile();
        if (!wcDbFile.exists()) {
            return;
        }

        final File wcDbBackupFile = new File(getWCDbFile().getAbsolutePath() + ".backup");
        SVNFileUtil.copy(wcDbFile, wcDbBackupFile, false, false);

        log("Backed up wc.db");
    }

    private void checkWorkingCopyConsistency() {
        final String wcDbPath = getWCDbFile().getAbsolutePath().replace('/', File.separatorChar);

        final ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(getSqlite3Command(), wcDbPath, "pragma integrity_check;");

        BufferedReader bufferedReader = null;
        try {
            final Process process = processBuilder.start();
            if (process != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                final String line = bufferedReader.readLine();
                if (line == null || !"ok".equals(line.trim())) {
                    final String notConsistentMessage = "SVN working copy database is not consistent.";

                    log(notConsistentMessage);
                    throw new RuntimeException(notConsistentMessage);
                }

                process.waitFor();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            SVNFileUtil.closeFile(bufferedReader);
        }
    }

    public File getWCDbFile() {
        return new File(getAdminDirectory(), ISVNWCDb.SDB_FILE);
    }

    private File getAdminDirectory() {
        return new File(getWorkingCopyDirectory(), SVNFileUtil.getAdminDirectoryName());
    }

    public void dispose() {
        if (clientManager != null) {
            clientManager.dispose();
            clientManager = null;
        }

        SVNFileUtil.closeFile(logger);
        logger = null;
    }

    private SVNClientManager getClientManager() {
        if (clientManager == null) {
            clientManager = SVNClientManager.newInstance();
        }
        return clientManager;
    }

    private TestOptions getTestOptions() {
        return testOptions;
    }

    public File getWorkingCopyDirectory() {
        return workingCopyDirectory;
    }

    private String getSqlite3Command() {
        return getTestOptions().getSqlite3Command();
    }

    public PrintWriter getLogger() {
        if (logger == null) {

            final File adminDirectory = getAdminDirectory();
            if (!adminDirectory.exists()) {
                //not checked out yet
                return null;
            }

            final File testLogFile = new File(adminDirectory, "test.log");

            FileWriter fileWriter = null;
            try {
                fileWriter = new FileWriter(testLogFile, true);
            } catch (IOException e) {
                SVNFileUtil.closeFile(fileWriter);
                throw new RuntimeException(e);
            }

            logger = new PrintWriter(fileWriter);
        }
        return logger;
    }

    private void log(String message) {
        final PrintWriter logger = getLogger();
        if (logger != null) {
            logger.println("[" + new Date() + "]" + message);
            logger.flush();
        }
    }

    private void beforeOperation() throws SVNException {
        log("");

        backupWcDbFile();
    }

    private void afterOperation() {
        checkWorkingCopyConsistency();

        log("Checked for consistency");
    }
}
