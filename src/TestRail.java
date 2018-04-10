import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import TestRailApi.APIClient;
import TestRailApi.APIException;
import java.util.HashMap;
import java.io.IOException;
import java.util.Iterator;

public class TestRail {

    private static String TestRailURL="https://tremend.testrail.net";
    private static String USER="sstefanescu@tremend.ro";
    private static String PASS="TestRail_0810";

    /**
     * Connects to TestRail and gets the results of the test run specified by the testNumber variable.
     * @param testNumber
     * @throws IOException
     * @throws APIException
     */
    public void runNo(String testNumber) throws IOException, APIException {
        APIClient client = new APIClient(TestRailURL);
        client.setUser(USER);
        client.setPassword(PASS);
        System.out.println("Test results for Test Run "+testNumber+":");
        JSONArray result = (JSONArray) client.sendGet("get_results_for_run/"+testNumber);
        System.out.println(result.toJSONString());
    }

    /**
     * Creates one or more milestones in a TestRail project, using a json array and a project ID
     * @param milestones
     * @param projectID
     * @throws IOException
     * @throws APIException
     */
    public void createMilestones(JSONArray milestones, String projectID) throws IOException, APIException {
        APIClient client = new APIClient(TestRailURL);
        client.setUser(USER);
        client.setPassword(PASS);

        String postRequest = "add_milestone/" + projectID;

        //iterate json array and create milestone for each object contained by it
        Iterator i = milestones.iterator();
        while (i.hasNext()){
            JSONObject jsonMilestone=(JSONObject) i.next();
            JSONObject serverResponse = (JSONObject) client.sendPost(postRequest, jsonMilestone);
            System.out.println("Milestone created for Jira version:" + jsonMilestone.get("name"));
        }
    }

    /**
     * Gets the milestones of a particular TestRail project, and returns them as a hashmap
     * The Hashmap contains the name and description of the milestone.
     * For further validation, "description" must contain string "Jira ID:<long>"
     * @param projectID
     * @throws IOException
     * @throws APIException
     */
    public HashMap<String, String> getMilestones(String projectID) throws IOException, APIException {
        APIClient client = new APIClient(TestRailURL);
        client.setUser(USER);
        client.setPassword(PASS);
        HashMap<String, String> list=new HashMap();
        JSONArray r = (JSONArray) client.sendGet("get_milestones/"+projectID);
        //System.out.println("Milestone received for project " + projectID);
        //System.out.println("Milestones jsonarray:"+r.toJSONString());

        //iterate through jsonArray to extract the Jira ID and Name of each milestone and put them into a hashmap
        //if uncommented, at the same time, update file with the list of IDs.
        Iterator i = r.iterator();
        String description=null;
        String name=null;
        int j=0;
        while (i.hasNext()) {
            JSONObject jsonObj = (JSONObject) i.next();
            name = (String) jsonObj.get("name");
            description = (String) jsonObj.get("description");
            //System.out.println(name+" "+description);
            //IMPORTANT: The description of each milestone must have the following pattern: "Jira ID:22134"
            if (description!=null) {
                String id = description.substring(8);
                list.put(name, id);
                j++;
            }
        }
        //list.forEach((k,v)-> System.out.println("Name:"+k+ ". ID:" + v));
        return list;
    }
}
