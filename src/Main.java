import org.json.simple.JSONArray;
import TestRailApi.APIException;
import java.io.IOException;
import java.util.HashMap;
public class  Main {
    public static void main(String[] args) throws Exception {
        syncJiraTestrailVersions("TEST", "20");
    }

    /**
     *  Syncs Jira versions with TestRail milestones: If a version has no corresponding milestone, it is created in TestRails.
     *  1."getProjectVersions" method reads all versions from Jira project. Returns JSON Array.
     *  2. "getMilestones" reads all existing "Jira related" milestones in TestRail project.
     *    "Jira Related" milestones: in their description field they have a "Jira ID:xxxxx", where "xxxxx" is
     *    the numeral ID of the corresponding Jira version
     *    "getMilestones" returns hashmap with names of TestRail milestones and their Jira IDs - if they have any.
     *  3. "getVersionsNameID" creates a hashmap with names and IDs from the JSON Array received from Jira.
     *  4. "compareHashes" compares the two hasmaps and returns a hashmap with unsynced Jira versions (versions that
     *    have no corresponding milestone ent in TestRail).
     *  5. "makeJsonMilestones" creates TestRail milestone JSON Objects for the unsynced Jira versions.
     *  6. "createMilestone" creates the new milestones.
     *
     *  TESTRAILS project: https://tremend.testrail.net/index.php?/projects/overview/20
     *  JIRA project: https://tremend.atlassian.net/secure/RapidBoard.jspa?rapidView=76&projectKey=TEST&view=planning
     *
     * @param jiraProject
     * @param testRailProject
     * @throws IOException
     * @throws APIException
     */
    private static void syncJiraTestrailVersions(String jiraProject, String testRailProject) throws IOException, APIException {

        TestRail testrail= new TestRail();
        Jira jira=new Jira();

        //connect to Jira and get all the the versions of the project
        JSONArray jiraVersions= null;
        try {
            jiraVersions = new JSONArray(jira.getProjectVersions(jiraProject));
        } catch (Exception e) {
            e.printStackTrace();
        }

        //convert jiraVersions array to a hashmap with names and IDs
        HashMap jiraVersionsNamesIDs= new HashMap(jira.getVersionsNameID(jiraVersions));

        //connect to Testrail and get the milestones' names and IDs
        HashMap milestonesNamesIDs=new HashMap(testrail.getMilestones("20"));

        //System.out.println("The Jira Versions hashmap:"+jiraVersionsNamesIDs.toString());
        //System.out.println("The milestones hashmap:"+milestonesNamesIDs.toString());

        //Creates a hashmap of the unsynced versions
        HashMap unsynced;
        unsynced = new HashMap(jira.compareHashes(jiraVersionsNamesIDs, milestonesNamesIDs));

        if (unsynced.size()==0)
            System.out.println("There are no unsynced Jira versions in the corresponding TestRails project.");
        else {
            System.out.println("There are "+unsynced.size()+" unsynced Jira versions.");
            System.out.println("The unsynced IDs are: " + unsynced.toString());

            //Creates an array of milestone JSONs
            JSONArray milestonesArray = new JSONArray(jira.makeJsonMilestones(jiraVersions, unsynced));
            //System.out.println("\n\nTHE ARRAY OF MILESTONE OBJECTS:"+milestonesArray.toJSONString());

            //Creates the new milestones in TestRail
            try {
                testrail.createMilestones(milestonesArray, testRailProject);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (APIException e) {
                e.printStackTrace();
            }
        }
    }
}