package buginducingcommitanalyzer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import buginducingcommitanalyzer.repowrapper.Commit;

public class BugInducingCommitLoader {
	
	private BufferedReader br;
	
	public BugInducingCommitLoader(String path){
		try {
			this.br = new BufferedReader(new FileReader(path));
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void load(HashMap<String, Commit > commits){
		String line;
		try {
			while((line = br.readLine()) !=null){
				String[] info=line.split("&");
				String commit_hash = info[0];
				//System.out.println("processing: "+ commit_hash);
				String author_name = info[1];
				//String author_date_unix_timestamp = info[2];
				//String author_email = info[3];
				String author_date = info[4];
				//String commit_message = info[5];
				//String fix = info[6];
				//String classification = info[7];
				//String linked = info[8];
				String contains_bug = info[9];
				String fixes = info[10];
				boolean isBugInducingCommit=false;
				boolean hasFix=false;
				
				
				List<Commit> fixedInCommits= new ArrayList<Commit>();
				if(contains_bug.equalsIgnoreCase("t")|| contains_bug.equalsIgnoreCase(" t ") ||
					contains_bug.equalsIgnoreCase("true")|| contains_bug.equalsIgnoreCase(" true ")	){
				    //System.out.println("\t it is a bug inducing commit");	   
					isBugInducingCommit=true;
					String[] fixedIn=null;
					if(!( fixes.equals(""))){
						fixes=fixes.replace("[", "").replace("]", "").replace("\"", "").replace(" ", "");
						fixedIn=fixes.split(",");
						for(String fixingCommit: fixedIn){
							if(commits.containsKey(fixingCommit)){
								fixedInCommits.add(commits.get(fixingCommit));
								commits.get(fixingCommit).setIsFixingCommit(true);
							}else{
								//System.out.println("ERROR: NO SUCH COMMIT: "+ fixingCommit);
								System.exit(0);
							}
						}
						hasFix=true;
						
					}else{
						//System.out.println("\t it does not have a fix");
					}
				}
				Commit commit= null;
				if(commits.containsKey(commit_hash)){
					 commit = commits.get(commit_hash);
				}else{
					commit = new Commit(commit_hash);
					commits.put(commit_hash, commit);
					//System.out.println("ERROR: NO SUCH COMMIT: "+ commit_hash + "  author: "+ author_name);
				}
				commit.setDate(author_date);
				commit.setBugInducingCommit(isBugInducingCommit);
				commit.setHasFix(hasFix);
				if(fixedInCommits.size() != 0){
					commit.getFixedIn().addAll(fixedInCommits);
				}
				String fileChangedInfo=info[17];
				if(!fileChangedInfo.contentEquals("NULL")){
					String[] changed = fileChangedInfo.split(",CAS_DELIMITER,");
					if(changed.length == 1){ // means that it did not contain  ,CAS_DELIMITER,
						changed = fileChangedInfo.split(",CAS_DELIMITER");
					}
					for(String fc:changed){
						if(fc.endsWith(".java")){
							commit.getChangedFiles().add(fc);
						}
					}
					if(commit.getChangedFiles().size() == 0){
						System.err.println("CHECK THIS COMMIT, NO JAVA FILE CHANGED: "+ commit_hash);
					}
					
				}
			}
			br.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}
