/*
 * ====================================================================
 * Copyright (c) 2004-2008 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.test.tests.merge.ext;

import java.util.Collection;
import java.util.LinkedList;
import java.io.File;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNRevisionRange;
import org.tmatesoft.svn.core.wc.SVNCopyTask;
import org.tmatesoft.svn.core.wc.SVNEditorAction;
import org.tmatesoft.svn.test.sandboxes.SVNSandboxFile;
import org.tmatesoft.svn.test.util.SVNTestDebugLog;

/**
 * @author TMate Software Ltd.
 * @version 1.2.0
 */
public class MergeAlreadyMergedFileTest extends AbstractExtMergeTest {

    public ISVNTestExtendedMergeCallback getFeatureModeCallback() {
        return new FeatureModeCallback();
    }

    public ISVNTestExtendedMergeCallback getReleaseModeCallback() {
        return new ReleaseModeCallback();
    }

    public String getDumpFile() {
        return null;
    }

    public Collection getInitialFS() {
        Collection fs = new LinkedList();
        fs.add(new SVNSandboxFile("A"));
        fs.add(new SVNSandboxFile("A/file", "this is A/file", false));
        return fs;
    }

    public void run() throws SVNException {
        fill();

        createWCs();
        initializeMergeCallback();

        getEnvironment().copy(getBranchFile("A/file"), SVNRevision.WORKING, getBranchFile("A/file2"), true, false, true);
        getEnvironment().commit(getBranchFile("A"), "A/file renamed to A/file2", SVNDepth.INFINITY);

        long change1 = getEnvironment().modifyAndCommit(getBranchFile("A/file2"));
        getEnvironment().modifyAndCommit(getBranchFile("A/file2"));
        long change3 = getEnvironment().modifyAndCommit(getBranchFile("A/file2"));
        long change4 = getEnvironment().modifyAndCommit(getBranchFile("A/file2"));

        Collection rangesToMerge = new LinkedList();
        rangesToMerge.add(new SVNRevisionRange(SVNRevision.create(change1 - 1), SVNRevision.create(change3)));
        getEnvironment().merge(getBranch().appendPath("A/file2", false), getTrunkFile("A/file"), rangesToMerge, SVNDepth.INFINITY, false, false);
        getEnvironment().commit(getTrunkWC(), "branch/A/file2 merged to trunk/A/file", SVNDepth.INFINITY);
        getEnvironment().update(getTrunkWC(), SVNRevision.HEAD, SVNDepth.INFINITY);

        getEnvironment().setEventHandler(SVNTestDebugLog.getEventHandler());

        rangesToMerge.clear();
        rangesToMerge.add(new SVNRevisionRange(SVNRevision.create(change1 - 1), SVNRevision.create(change4)));
        prepareMerge(getBranch(), getTrunkWC(), SVNRevision.create(change1 - 1), SVNRevision.create(change4));
        getEnvironment().merge(getBranch(), getTrunkWC(), rangesToMerge, SVNDepth.INFINITY, false, false);

        getEnvironment().getFileContents(getTrunkFile("A/file"), System.out);
    }

// ###############  FEATURE MODE  ###################    

    private class FeatureModeCallback implements ISVNTestExtendedMergeCallback {

        public void prepareMerge(SVNURL source, File target, SVNRevision start, SVNRevision end) throws SVNException {
        }

        public SVNCopyTask getTargetCopySource(SVNURL sourceUrl, long sourceRevision, long sourceMergeToRevision, SVNURL targetUrl, long targetRevision) throws SVNException {
            return null;
        }

        public SVNURL[] getTrueMergeTargets(SVNURL sourceUrl, long sourceRevision, long sourceMergeToRevision, SVNURL targetUrl, long targetRevision, SVNEditorAction action) throws SVNException {
            return new SVNURL[0];
        }

        public SVNURL transformLocation(SVNURL sourceUrl, long sourceRevision, long targetRevision) throws SVNException {
            return null;
        }
    }

// ###############  RELEASE MODE  ###################    

    private class ReleaseModeCallback implements ISVNTestExtendedMergeCallback {

        public SVNCopyTask getTargetCopySource(SVNURL sourceUrl, long sourceRevision, long sourceMergeToRevision, SVNURL targetUrl, long targetRevision) {
            return null;
        }

        public SVNURL[] getTrueMergeTargets(SVNURL sourceUrl, long sourceRevision, long sourceMergeToRevision, SVNURL targetUrl, long targetRevision, SVNEditorAction action) throws SVNException {
            if (sourceUrl.getPath().endsWith("branch/A/file2")) {
                return new SVNURL[]{getTrunk().appendPath("A/file", false)};
            }
            return null;
        }

        public SVNURL transformLocation(SVNURL sourceUrl, long sourceRevision, long targetRevision) throws SVNException {
            return null;
        }

        public void prepareMerge(SVNURL source, File target, SVNRevision start, SVNRevision end) throws SVNException {
        }
    }
}