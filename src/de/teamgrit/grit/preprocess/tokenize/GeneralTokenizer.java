/*
 * Copyright (C) 2014 Team GRIT
 * 
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.teamgrit.grit.preprocess.tokenize;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipException;

import org.apache.commons.io.FilenameUtils;

import de.teamgrit.grit.preprocess.Student;
import de.teamgrit.grit.preprocess.archivehandling.ArchiveHandler;
import de.teamgrit.grit.preprocess.archivehandling.ZipfileHandler;
import de.teamgrit.grit.util.config.NoProperParameterException;

/**
 * The Tokenizer iterates through a directory specified by a {@link Path} and
 * returns all submissions {@link Submission} matching the
 * {@link SubmissionStructure}.
 * 
 * @author <a href="mailto:marvin.guelzow@uni-konstanz.de">Marvin Guelzow</a>
 * @author <a href="mailto:eike.heinz@uni-konstanz.de">Eike Heinz</a>
 */

public class GeneralTokenizer implements Tokenizer {

    private static final int m_maxDirectoryDepth = 10;

    private List<Path> m_emptyLocations = null;

    private final Logger m_log = Logger.getLogger("systemlog");

    // these objects indicate the files we are searching for by specifying the
    // file extension
    private final String m_sourceSuffixRegex;
    private final String m_archiveRegex;

    /**
     * Instantiates a new Tokenizer. Arguments are Regexes that match source
     * files and archive files respectively.
     */
    public GeneralTokenizer(String suffixRegexes, String newArchiveRegexes) {
        m_sourceSuffixRegex = suffixRegexes;
        m_archiveRegex = newArchiveRegexes;
    }

    /**
     * This method is required by the {@link de.teamgrit.grit.preprocess.Preprocessors} to
     * identify students that haven't submitted anything.
     * 
     * @return a list of student that have not submitted anything
     */
    @Override
    public List<Path> getEmptySubmissions() {
        return m_emptyLocations;
    }

    @Override
    public List<Submission> exploreSubmissionDirectory(
            SubmissionStructure submissionStructure, Path location)
            throws MaximumDirectoryDepthExceededException {

        // list to log all students that have not submitted anything
        m_emptyLocations = new ArrayList<>();
      

        // first make sure we have got stuff to check at all
        // Mailfetcher sets location to null, if no submission was available, therefore it has to be taken care here.
        try {
        	if ((location.toFile().listFiles() == null)
                    || (location.toFile().listFiles().length == 0) || location == null) {
                // then return nothing.
                return new LinkedList<>();
            }
		} catch (NullPointerException e) {
			return new LinkedList<>();
		}
        
       
        List<Submission> foundSubmissions = new LinkedList<>();

        // We are now at TOPLEVEL, which is location
        // now we recursively traverse along the given structure
        
        List<Path> allSubmissionPaths = traverse(
                submissionStructure.getStructure(), location);
                         
        // For each file we found we add a submission object
        
        int i = 0;
        
        for (Path submissionFile : allSubmissionPaths) {
        	Submission submission = new Submission(submissionFile, new Student(
                    "Unknown" + i));
            foundSubmissions.add(submission);
            i++;
        }
        
        return foundSubmissions;
    }

    /**
     * Convenience overload for real traverse method defined below.
     * 
     * @param structure
     *            See other method.
     * @param location
     *            See other method.
     * @return See other method.
     * @throws MaximumDirectoryDepthExceededException
     *             When other traverse method throws this method passes it
     *             along.
     */
    private List<Path> traverse(List<String> structure, Path location)
            throws MaximumDirectoryDepthExceededException {
        return traverse(structure, 1, location);
    }

    /**
     * Traverses a directory tree, only considering directories that match the
     * ones specified in {@link SubmissionStructure}. When reaching the lowest
     * level, submissions are gathered.
     * 
     * @param structure
     *            A StrcutureObj containing a description of how folders
     *            containing the submissions are arranged.
     * @param level
     *            how deep we have descended into the hierarchy. This starts
     *            out with 1 and is then used internally to track the depth of
     *            folders. Also used to ensure a maximum recursion depth.
     * @param location
     *            Which directory is to be scanned.
     * @return A list of all matching files/folders we have found.
     * @throws MaximumDirectoryDepthExceededException
     *             when maxDirectoryDepth is exceeded while traversing
     *             directories.
     */
    private List<Path> traverse(List<String> structure, int level, Path location)
            throws MaximumDirectoryDepthExceededException {

        m_log.info("traverse: " + location.toString());

        // If we went too deep, we abort here in order to avoid exploding our
        // stackspace
        if (level >= m_maxDirectoryDepth) {
            throw new MaximumDirectoryDepthExceededException(
                    "Encountered more than " + m_maxDirectoryDepth
                            + " Directories in " + location.toString()
                            + "\nProgram returned Cthulhu.");
        }

        // If we have reached the bottom, we can scan for files.
        if ("SUBMISSION".equals(structure.get(level))) {
            // look for files.
        	m_log.info("Bottom reached");
        	m_log.info("location of Submission" + location.toString());
            m_log.config("Bottomed out in " + location);
            List<Path> submission = extractSubmissionFiles(location);
            // log students without a submission
            if (submission.isEmpty()) {
                m_log.info("Nothing found in " + location);
                m_emptyLocations.add(location);
            }
            return submission;
        }

        List<Path> foundSubmissions = new LinkedList<>();

        // ensure that empty dirs are handled properly
        try {
        	if ((location.toFile().listFiles() == null)
                || (location.toFile().listFiles().length == 0)) {
            m_log.info("No files in " + location.toString());
            return new LinkedList<>();
        } 	
		} catch (NullPointerException e) {
			return new LinkedList<>();
		}
        

        // If we are not too deep and not in the final level, go through all
        // directories here and go one level deeper.
        for (File currentFile : location.toFile().listFiles()) {
            m_log.info("looking at " + currentFile.toString());
            if (currentFile.isDirectory()) {
            	m_log.info("isDirectory " + currentFile.toString());
                // does this directory match our structure spec?
            	m_log.info("Match " + currentFile.getName());
            	m_log.info("mit " + structure.get(level));
                if (currentFile.getName().matches(structure.get(level))) {
                	// if so, traverse it and collect everything it returns.
                	m_log.info("Match erfolgreich");
                	m_log.info("file" + currentFile.toPath().toString());
                    foundSubmissions.addAll(traverse(structure, (level + 1),
                            currentFile.toPath()));
                    
                } else {
                    m_log.info("Unexpected file: " + currentFile.toString()
                            + " to " + structure.get(level));
                }
            }
        }
        //Ausgabe falls travers fehlschlägt
        if(foundSubmissions.size() == 0)
        	m_log.info("no Submissions found");
        
        //Ausgabe der gefundenen Submissions
        for (int i= 0; i < foundSubmissions.size(); i++)
        	m_log.info("Submissions found" + foundSubmissions.get(i).toString());
        return foundSubmissions;
    }

    /**
     * On the submission level, this function gathers all relevant files and
     * returns them.
     * 
     * @param location
     *            Where to look for files
     * @return Files matching suffixRegex in the given folder
     */
    private List<Path> extractSubmissionFiles(Path location) {
        List<Path> submissionFiles = new LinkedList<>();

        m_log.info("Extracting files.");

        // Check if our location has any submissions files (as recognized by
        // their suffix. if so, note it.
        for (File currentFile : location.toFile().listFiles()) {

            // unpack archives
            if (currentFile.toString().matches(m_archiveRegex)) {
            	m_log.info("unpack archives: " + currentFile.toString());
                try {
                    // the number indicates to which level of a zipfile nested
                    // zipfiles will be extracted
                    ArchiveHandler unzipper = new ZipfileHandler(5, Paths.get(
                            ".").toFile());

                    Path unzippedDir = Paths.get(FilenameUtils
                            .removeExtension(currentFile.toString()));

                    unzipper.extractZip(currentFile, unzippedDir.toFile());

                    submissionFiles.add(unzippedDir);

                } catch (FileNotFoundException | ZipException e) {
                    m_log.info("Error while unzipping ilias submission"
                            + e.getMessage());
                } catch (NoProperParameterException e) {
                    m_log.info("Bad parameters for zip." + e.getMessage());
                }
            } else if (currentFile.toString().matches(m_sourceSuffixRegex)) {
                m_log.info("Found: " + currentFile.toString());
                if (!submissionFiles.contains(location)) {
                    submissionFiles.add(location);
                }
            } else {
                m_log.info("found invalid file: " + currentFile.toString());
            }
        }
        if(submissionFiles.size()==0)
        	m_log.info("no Submissionfiles added");
        return submissionFiles;
    }
}
