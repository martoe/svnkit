package org.tmatesoft.svn.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.wc.SVNFileListUtil;
import org.tmatesoft.svn.core.internal.wc.SVNFileUtil;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.ISVNReporter;
import org.tmatesoft.svn.core.io.ISVNReporterBaton;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;

public class StressTest {

    @Ignore("Temporarily ignored")
    @Test
    public void testWorkingCopy() throws Exception {
        final TestOptions testOptions = TestOptions.getInstance();
        Assume.assumeNotNull(testOptions.getRepositoryUrl());

        final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testWorkingCopy", testOptions);
        try {
            final WorkingCopy workingCopy = sandbox.checkoutWorkingCopy();

            runAdds(workingCopy);

            runRevert(workingCopy);

            runDeletes(workingCopy);

            runRevert(workingCopy);

            runSetProperties(workingCopy);

            runRevert(workingCopy);
        } finally {
            sandbox.dispose();
        }
    }

    @Test
    public void testUpdates() throws Exception {
        final TestOptions testOptions = TestOptions.getInstance();
        Assume.assumeNotNull(testOptions.getRepositoryUrl());

        final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testUpdates", testOptions);
        try {
            final WorkingCopy workingCopy = sandbox.checkoutWorkingCopy();

            runUpdates(workingCopy, testOptions.getLargeUpdateStep());
        } finally {
            sandbox.dispose();
        }
    }

    @Ignore("Temporarily ignored")
    @Test
    public void testCommits() throws Exception {
        final TestOptions testOptions = TestOptions.getInstance();
        Assume.assumeNotNull(testOptions.getRepositoryUrl());

        final Sandbox sandbox = Sandbox.createWithCleanup(getTestName() + ".testCommits", testOptions);
        try {
            final SVNURL originalRepositoryUrl = testOptions.getRepositoryUrl();
            final SVNURL targetRepositoryUrl = sandbox.createSvnRepository();

            translateRevisionByRevision(sandbox, originalRepositoryUrl, targetRepositoryUrl);
        } finally {
            sandbox.dispose();
        }
    }

    private void runUpdates(WorkingCopy workingCopy, long stepForLargeUpdatesInRevisions) throws SVNException {
        final List<SVNLogEntry> logEntries = getLogEntries(workingCopy.getRepositoryUrl());
        Assert.assertTrue(logEntries.size() > 0);

        updateToOldestRevisionAndBack(workingCopy, logEntries, 1);
        updateToOldestRevisionAndBack(workingCopy, logEntries, stepForLargeUpdatesInRevisions);

        workingCopy.updateToRevision(logEntries.get(logEntries.size() - 1).getRevision());
    }

    private void updateToOldestRevisionAndBack(WorkingCopy workingCopy, List<SVNLogEntry> allLogEntries, long stepInRevisions) throws SVNException {
        for (int i = allLogEntries.size() - 1; i >= 0; i-=stepInRevisions) {
            workingCopy.updateToRevision(allLogEntries.get(i).getRevision());
        }

        for (int i = 1; i < allLogEntries.size(); i+= stepInRevisions) {
            workingCopy.updateToRevision(allLogEntries.get(i).getRevision());
        }
    }

    private void runAdds(WorkingCopy workingCopy) throws SVNException {
        final File originalDirectory = workingCopy.findAnyDirectory();
        File directory = originalDirectory;

        final File originalAnotherDirectory = workingCopy.findAnotherDirectory(directory);
        File anotherDirectory = originalAnotherDirectory;

        for (int i = 0; i < 5; i++) {
            SVNFileUtil.copyDirectory(directory, new File(anotherDirectory, directory.getName()), false, null);

            File tmp = directory;
            directory = anotherDirectory;
            anotherDirectory = tmp;
        }

        workingCopy.add(new File(originalDirectory, originalAnotherDirectory.getName()));
        workingCopy.add(new File(originalAnotherDirectory, originalDirectory.getName()));
    }

    private void runRevert(WorkingCopy workingCopy) throws SVNException {
        workingCopy.revert();
    }

    private void runDeletes(WorkingCopy workingCopy) throws SVNException {
        final List<File> childrenList = workingCopy.getChildren();

        for (File child : childrenList) {
            workingCopy.delete(child);
        }
    }

    private void runSetProperties(WorkingCopy workingCopy) throws SVNException {
        final File workingCopyDirectory = workingCopy.getWorkingCopyDirectory();

        setProperties(workingCopy, workingCopyDirectory, 0);
    }

    private int setProperties(WorkingCopy workingCopy, File directory, int counter) throws SVNException {
        final File[] children = directory.isDirectory() ? SVNFileListUtil.listFiles(directory) : null;

        if (children != null) {
            for (File child : children) {
                if (!child.getName().equals(SVNFileUtil.getAdminDirectoryName())) {
                    workingCopy.setProperty(child, "property" + counter, SVNPropertyValue.create(child.getAbsolutePath()));
                    counter++;
                    counter = setProperties(workingCopy, child, counter);
                }
            }
        }
        return counter;
    }

    private String getTestName() {
        return getClass().getSimpleName();
    }

    private void translateRevisionByRevision(Sandbox sandbox, SVNURL originalRepositoryUrl, SVNURL targetRepositoryUrl) throws SVNException {
        final WorkingCopy workingCopy = sandbox.checkoutWorkingCopy(targetRepositoryUrl);

        final List<SVNLogEntry> logEntries = getLogEntries(originalRepositoryUrl);
        long previousRevision = -1;

        SVNRepository svnRepository = null;
        try {
            svnRepository = SVNRepositoryFactory.create(originalRepositoryUrl);

            for (SVNLogEntry logEntry : logEntries) {
                final long revision = logEntry.getRevision();

                final ISVNEditor editor = new WorkingCopyEditor(workingCopy);
                final ISVNReporterBaton reporter = new RevisionByRevisionReporter(previousRevision, revision);

                svnRepository.update(revision, "", SVNDepth.INFINITY, false, reporter, editor);

                final long committedRevision = workingCopy.commit(logEntry.getMessage());
                workingCopy.updateToRevision(committedRevision);

                previousRevision = revision;
            }
        } finally {
            if (svnRepository != null) {
                svnRepository.closeSession();
            }
        }
    }

    private List<SVNLogEntry> getLogEntries(SVNURL originalRepositoryUrl) throws SVNException {
        SVNRepository svnRepository = null;
        try {
            svnRepository = SVNRepositoryFactory.create(originalRepositoryUrl);

            return (List<SVNLogEntry>) svnRepository.log(new String[]{""}, new ArrayList<SVNLogEntry>(), 0, svnRepository.getLatestRevision(), false, true);
        } finally {
            if (svnRepository != null) {
                svnRepository.closeSession();
            }
        }
    }

    private class RevisionByRevisionReporter implements ISVNReporterBaton {

        private final long previousRevision;
        private final long revision;

        public RevisionByRevisionReporter(long previousRevision, long revision) {
            this.previousRevision = previousRevision;
            this.revision = revision;
        }

        public void report(ISVNReporter reporter) throws SVNException {
            final boolean startEmpty = previousRevision == -1;
            reporter.setPath("", null, startEmpty ? revision : previousRevision, SVNDepth.INFINITY, startEmpty);
            reporter.finishReport();
        }
    }
}
