/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.internal.file;

import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.gradle.api.UncheckedIOException;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.util.FileSet;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.file.RelativePath;
import org.gradle.util.HashUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

public class ZipFileTree extends AbstractFileTree {
    private final File zipFile;
    private final File tmpDir;

    public ZipFileTree(File zipFile, File tmpDir) {
        this.zipFile = zipFile;
        String expandDirName = String.format("%s_%s", zipFile.getName(), HashUtil.createHash(zipFile.getAbsolutePath()));
        this.tmpDir = new File(tmpDir, expandDirName);
    }

    public String getDisplayName() {
        return String.format("ZIP '%s'", zipFile);
    }

    @Override
    protected Collection<FileSet> getAsFileSets() {
        visitAll();
        return zipFile.exists() ? Collections.singleton(new FileSet(tmpDir, null)) : Collections.<FileSet>emptyList();
    }

    public FileTree visit(FileVisitor visitor) {
        if (!zipFile.exists()) {
            return this;
        }
        if (!zipFile.isFile()) {
            throw new InvalidUserDataException(String.format("Cannot expand %s as it is not a file.", this));
        }

        AtomicBoolean stopFlag = new AtomicBoolean();

        try {
            ZipFile zip = new ZipFile(zipFile);
            try {
                Enumeration entries = zip.getEntries();
                while (!stopFlag.get() && entries.hasMoreElements()) {
                    final ZipEntry entry = (ZipEntry) entries.nextElement();
                    if (entry.isDirectory()) {
                        visitor.visitDir(new DetailsImpl(entry, zip, stopFlag));
                    } else {
                        visitor.visitFile(new DetailsImpl(entry, zip, stopFlag));
                    }
                }
            } finally {
                zip.close();
            }
        } catch (Exception e) {
            throw new GradleException(String.format("Could not expand %s.", this), e);
        }

        return this;
    }

    private class DetailsImpl extends AbstractFileTreeElement implements FileVisitDetails {
        private final ZipEntry entry;
        private final ZipFile zip;
        private final AtomicBoolean stopFlag;
        private File file;

        public DetailsImpl(ZipEntry entry, ZipFile zip, AtomicBoolean stopFlag) {
            this.entry = entry;
            this.zip = zip;
            this.stopFlag = stopFlag;
        }

        public String getDisplayName() {
            return String.format("zip entry %s!%s", zipFile, entry.getName());
        }

        public void stopVisiting() {
            stopFlag.set(true);
        }

        public File getFile() {
            if (file == null) {
                file = new File(tmpDir, entry.getName());
                copyTo(file);
            }
            return file;
        }

        public long getLastModified() {
            return entry.getTime();
        }

        public boolean isDirectory() {
            return entry.isDirectory();
        }

        public long getSize() {
            return entry.getSize();
        }

        public InputStream open()  {
            try {
                return zip.getInputStream(entry);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        public RelativePath getRelativePath() {
            return new RelativePath(true, entry.getName().split("/"));
        }
    }
}
