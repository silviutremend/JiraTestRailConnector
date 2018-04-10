import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.net.HttpURLConnection;
import java.util.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

public class Jira {
    private final String USER_AGENT = "Mozilla/5.0";
    private String auth = "sstefanescu:Atla_0810";
    private String JiraURL="https://tremend.atlassian.net/rest/api/2/";

    /**
     * Gets a JIRA element and displays its details - based on the element ID.
     * @param jiraElement
     * @throws Exception
     */
    public void getIssue(String jiraElement) throws Exception {

        String url = JiraURL+"issue/" + jiraElement;
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        // Base64 encoding of the authentication part
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
        auth = new String(encodedAuth);
        //set authentication header
        con.setRequestProperty("Authorization", "Basic " + auth);

        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'GET' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);

        //to avoid errors, process the data stream only if connection is OK
        if (responseCode == 200) {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            //print result
            String jsonString = response.toString();
            String replace1 = jsonString.replace("\",", "\",\n");
            String replace2 = replace1.replace("},", "\n},\n");
            String replace3 = replace2.replace("\":{", "\":\n  {");
            System.out.println(replace3);
        }
    }

    /**
     * Gets all of the the existing versions of a particular Project, identified with its Jira ID
     * Returns a JSON Array containing the JSON objects describing the Jira versions
     * @param projectID
     * @throws Exception
     */
    public JSONArray getProjectVersions(String projectID) throws Exception {

        //prepare connection variables and connect
        String url = JiraURL + "project/" + projectID + "/versions";
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        JSONArray jsonArray = new JSONArray();
        String fileName = "Project_" + projectID + "_versions.txt";
        System.out.println("Connecting to Jira at " + url + "...");

        // optional default is GET
        con.setRequestMethod("GET");

        // Base64 encoding of the authentication variables
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
        auth = new String(encodedAuth);
        //set authentication header
        con.setRequestProperty("Authorization", "Basic " + auth);

        //check server response code
        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'GET' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);

        //process the data stream only if connection is OK
        if (responseCode == 200) {
            System.out.println("Connection OK. \nProcessing server response.");
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            //turn response string into JSON Array using method based on jsonParse library
            jsonArray = parseJSONString(response.toString());
            //System.out.println(response.toString());
        }
        return jsonArray;
    }


    public HashMap getVersionsNameID(JSONArray jsonArray){
        //iterate through jsonArray to extract the ID and Name of each version and put them into a hashmap
        //returns the hashmap
        //if uncommented, it also writes IDs to a file.
        Iterator i = jsonArray.iterator();
        String versionID=null;
        String versionName=null;
        HashMap <String, String> list=new HashMap<String, String>();
        long projID=0;
        int j=0;
        //BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true));
        while (i.hasNext()) {
            JSONObject jsonObj = (JSONObject) i.next();
            //projID=(Long)jsonObj.get("projectId");
            versionID=(String)jsonObj.get("id");
            versionName=(String)jsonObj.get("name");
            list.put(versionName, versionID);

            //System.out.println("\nProject "+projID);
            //System.out.println("Version name: "+);
            //System.out.println("ID: "+versionID);
            //System.out.println("URL: "+jsonObj.get("self")+"\n");
            //write version ID to file
            //writer.append(versionID);
            //writer.newLine();
            j++;
        }
        //writer.close();
        //System.out.println("The test has "+j+"versions.");
        //System.out.println("Latest version for current test: "+versionID);
        return list;
    }

    /**
     * SS: Turns a JSON String containing the test versions into a JSON Array;
     * The JSON string is based on the JIRA server response after an API call.
     * @param jsonString
     * @return JSONArray
     * @throws ParseException
     */
    public JSONArray parseJSONString(String jsonString) throws ParseException{
        JSONParser parser = new JSONParser();
        //convert from JSON string to JSONObject
        JSONArray json= new JSONArray();
            try
        {
            json = (JSONArray) parser.parse(jsonString);
        } catch(ParseException e)
            {
                e.printStackTrace();
            }
        return json;
    }

    /**
     * Compares two hashmaps. If the second one lacks one of the ID's in the first one,
     * it returns a hash with the missing values
     * @param jiraVersions
     * @param milestones
     * @return
     */
    public HashMap<String, String> compareHashes(HashMap <String, String> jiraVersions, HashMap <String, String> milestones){
        HashMap unsynced=new HashMap<String, String>();
        int i=0;
        jiraVersions.forEach((String k, String v) ->{
            //if milestones don't contain a certain ID from jira versins, add the ID to the unsinced list
            if (!milestones.containsValue(v)) unsynced.put(k, v);
        });

        return unsynced;
    }


    /**
     * Based on the hashmap with unsynced Jira versions and the big JSON Array of all jira versions,
     * this method builds a TestRail API json object that describes a milestone for each unsynced Jira version
     * All these objects are bundled into a JSON Array which is then returned.
     *
     * @param jiraVersions
     * @param unsynced
     * @return
     */
    public JSONArray makeJsonMilestones (JSONArray jiraVersions, HashMap <String, String> unsynced){

        JSONArray jsonArrayMilestones = new JSONArray();
        String versionID=null;
        String versionName=null;
        long projID=0;
        int j=0;

        //iterate the hash with unsynced jira version IDs.
        unsynced.forEach((String k, String v) ->{

            //iterate the jiraVersions array to compare current unsynced version id (v) with all jira version jsons
            Iterator i = jiraVersions.iterator();
            while (i.hasNext()) {
                //extract json object from incoming array
                JSONObject jiraJsonVersion = (JSONObject) i.next();
                //build json object if an unsynced version is spotted
                if (v==(String)jiraJsonVersion.get("id")){
                //create json object for milestones using jira version json
                JSONObject milestone = new JSONObject();
                milestone.put("name",jiraJsonVersion.get("name").toString());
                //the milestone's description will follow the "Jira ID:22134" pattern
                milestone.put("description","Jira ID:"+jiraJsonVersion.get("id").toString());
                //Integer due_on=(Integer)jiraJsonVersion.get("releaseDate");
                //milestone.put("due_on",due_on);
                milestone.put("parent_id",null);
                milestone.put("start_on",null);
                jsonArrayMilestones.add(milestone);
                }
            }
        });
        return jsonArrayMilestones;
    }
}

