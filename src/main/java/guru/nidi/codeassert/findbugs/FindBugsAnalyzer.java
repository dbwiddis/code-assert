/*
 * Copyright (C) 2015 Stefan Niederhauser (nidin@gmx.ch)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package guru.nidi.codeassert.findbugs;

import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.config.UserPreferences;
import guru.nidi.codeassert.Analyzer;
import guru.nidi.codeassert.AnalyzerConfig;

import java.io.IOException;
import java.util.*;

/**
 *
 */
public class FindBugsAnalyzer implements Analyzer<Collection<BugInstance>> {
    private static final Comparator<BugInstance> BUG_SORTER = new Comparator<BugInstance>() {
        @Override
        public int compare(BugInstance b1, BugInstance b2) {
            final int prio = b1.getPriority() - b2.getPriority();
            if (prio != 0) {
                return prio;
            }
            final int rank = b2.getBugRank() - b1.getBugRank();
            if (rank != 0) {
                return rank;
            }
            return b1.getType().compareTo(b2.getType());
        }
    };

    private final AnalyzerConfig config;
    private final BugCollector bugCollector;

    public FindBugsAnalyzer(AnalyzerConfig config, BugCollector bugCollector) {
        this.config = config;
        this.bugCollector = bugCollector;
    }

    public Collection<BugInstance> analyze() {
        final Project project = createProject();
        final BugCollectionBugReporter bugReporter = createReporter(project);
        final FindBugs2 findBugs = createFindBugs(project, bugReporter);
        try {
            findBugs.execute();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Problem executing FindBugs.", e);
        }
        return createBugList(bugReporter);
    }

    private Project createProject() {
        final Project project = new Project();
        for (final String loc : config.getCodeLocations()) {
            project.addFile(loc);
        }
        final String pathSeparator = System.getProperty("path.separator");
        final String classPath = System.getProperty("java.class.path");
        for (final String entry : classPath.split(pathSeparator)) {
            project.addAuxClasspathEntry(entry);
        }
        return project;
    }

    private BugCollectionBugReporter createReporter(Project project) {
        final BugCollectionBugReporter bugReporter = new BugCollectionBugReporter(project);
        bugReporter.setPriorityThreshold(Priorities.LOW_PRIORITY);
        bugReporter.setApplySuppressions(true);
        return bugReporter;
    }

    private FindBugs2 createFindBugs(Project project, BugCollectionBugReporter bugReporter) {
        final FindBugs2 findBugs = new FindBugs2();
        findBugs.setProject(project);
        findBugs.setBugReporter(bugReporter);
        findBugs.setDetectorFactoryCollection(DetectorFactoryCollection.instance());
        findBugs.setUserPreferences(UserPreferences.createDefaultUserPreferences());
        return findBugs;
    }

    private Collection<BugInstance> createBugList(BugCollectionBugReporter bugReporter) {
        final Collection<BugInstance> bugs = bugReporter.getBugCollection().getCollection();
        final ArrayList<BugInstance> sorted = new ArrayList<>(bugs);
        Collections.sort(sorted, BUG_SORTER);
        List<BugInstance> filtered = new ArrayList<>();
        for (BugInstance bug : sorted) {
            if (bugCollector.accept(bug) && config.getPackageCollector().accept(bug.getPrimaryClass().getPackageName())) {
                filtered.add(bug);
            }
        }
        return filtered;
    }
}