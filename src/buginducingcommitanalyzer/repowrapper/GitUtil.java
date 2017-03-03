package buginducingcommitanalyzer.repowrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.filter.PathFilter;

public class GitUtil {
	
	public static AbstractTreeIterator prepareTreeParser(Repository repository, String objectId) throws IOException {
		// from the commit we can build the tree which allows us to construct the TreeParser
	   //noinspection Duplicates
	   try (RevWalk walk = new RevWalk(repository)) {
		   RevCommit commit = walk.parseCommit(ObjectId.fromString(objectId));
	       RevTree tree = walk.parseTree(commit.getTree().getId());
	       CanonicalTreeParser aParser = new CanonicalTreeParser();
	       try (ObjectReader oldReader = repository.newObjectReader()) {
	             aParser.reset(oldReader, tree.getId());
	       }
	       walk.dispose();
	       return aParser;
	    }
	}
	
	// almost the same as the command: git diff oldHash^:FILE-PATH newHas:FILE-PATH
	public static String getFileDiff(Repository repo, String oldCommitId, String newCommitId, String relPathToFile){
		String diffAsString = null;
		try {
			 Git git = new Git(repo);
			 AbstractTreeIterator oldTreeParser = GitUtil.prepareTreeParser(repo, oldCommitId);//8d7787054640a15d7a4783f87149aef243ffb6a6
			 AbstractTreeIterator newTreeParser = GitUtil.prepareTreeParser(repo, newCommitId);// e90650e63c96d40ac240b2009ba218751386925c
			 List<DiffEntry> diff = git.diff().
	             setOldTree(oldTreeParser).
	             setNewTree(newTreeParser).
	             setPathFilter(PathFilter.create(relPathToFile)).//   should start like: DesA/src/no/simula/des/service/impl/XmlRpcPeopleService.java"
	             call();
			 for (DiffEntry entry : diff) {
				// System.out.println("Entry: " + entry + ", from: " + entry.getOldId() + ", to: " + entry.getNewId());
				 ByteArrayOutputStream baos = new ByteArrayOutputStream();
				 DiffFormatter formatter = new DiffFormatter(baos);
				 formatter.setRepository(repo);
	             formatter.format(entry);
	             diffAsString = baos.toString( java.nio.charset.StandardCharsets.UTF_8.name());
			 }
		} catch (IOException e) {
			e.printStackTrace();
		} catch (GitAPIException e) {
			e.printStackTrace();
		}
		 return diffAsString;
	 }

}
