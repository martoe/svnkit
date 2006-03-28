/*
 * ====================================================================
 * Copyright (c) 2004-2006 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://tmate.org/svn/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.internal.wc;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.io.ISVNWorkspaceMediator;

/**
 * @version 1.0
 * @author TMate Software Ltd.
 */
public class SVNImportMediator implements ISVNWorkspaceMediator {

    private Map myLocations;

    public SVNImportMediator() {
        myLocations = new HashMap();
    }

    public String getWorkspaceProperty(String path, String name)  throws SVNException {
        return null;
    }

    public void setWorkspaceProperty(String path, String name, String value)
            throws SVNException {
    }

    public OutputStream createTemporaryLocation(String path, Object id) throws SVNException {
        File tmpFile = SVNFileUtil.createTempFile(SVNPathUtil.tail(path), ".tmp");
        OutputStream os = SVNFileUtil.openFileForWriting(tmpFile);
        myLocations.put(id, tmpFile);
        return os;
    }

    public InputStream getTemporaryLocation(Object id) throws SVNException {
        File file = (File) myLocations.get(id);
        if (file != null) {
            return SVNFileUtil.openFileForReading(file);
        }
        return null;
    }

    public long getLength(Object id) throws SVNException {
        File file = (File) myLocations.get(id);
        if (file != null) {
            return file.length();
        }
        return 0;
    }

    public void deleteTemporaryLocation(Object id) {
        File file = (File) myLocations.remove(id);
        if (file != null) {
            file.delete();
        }
    }
}
