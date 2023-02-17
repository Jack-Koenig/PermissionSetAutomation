import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.*;
import java.net.*;
import java.util.*;

public class OpenCSV {
    public static void main(String[] args) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Enter ApplicationInformation File Name: ");
        Scanner r = new Scanner(new File(reader.readLine()));
        String file_name = r.nextLine();
        System.out.println("ACCESS MATRIX FILE NAME: " + file_name);
        String app_url = r.nextLine();
        System.out.println("APPLICATION BASE URL: " + app_url);
        String secret_key = r.nextLine();
        System.out.println("SECRET KEY: " + secret_key);
        String client_key = r.nextLine();
        System.out.println("CLIENT KEY: " + client_key);

        List<permissionset> permissionsetlist = new ArrayList<permissionset>();
        Scanner sc = new Scanner(new File(file_name));
        sc.useDelimiter(",");
        String bearer_key="";
        int col = 0, row = 0; // Max Columns 19, Max Rows 60
        String[][] excelsheet = new String[20][61];
        System.out.println("Reading excel file....");
        String temp = "";
        while (sc.hasNext()) {
            //reading the csv file
            if (col == 18) //reached last column, next row and repeat
            {
                temp = sc.next();
                String[] s = new String[2];
                s[0] = "";
                s[1] = "";
                s=temp.split("\n");
                excelsheet[col][row] = s[0].substring(0,s[0].length()-1);
                col = 0;
                row++;
                if(s.length>1) {
                    excelsheet[col][row] = s[1];
                    col++;
                }
            }
            else {
                temp = sc.next();
                excelsheet[col][row] = temp;
                col++;
            }
        }
        sc.close();  //closes the scanner
        System.out.println("System finished reading excel file.");
        for (int i = 4; i < 19; i++) { //only reading the permission set names
            //System.out.println("Test: " + excelsheet[i][0]);
            if (excelsheet[i][0].compareTo("[Null]_Permission_Set") != 0) {
                //System.out.println("Add: " + excelsheet[i][0]);
                permissionsetlist.add(new permissionset(excelsheet[i][0])); // creating list of permission set names
            }
        } //finished creating a list for each permission set

        //add workflows and steps to each permission set
        int stepaccesscounter = 0;
        for(permissionset p: permissionsetlist){
            for (int y = 1; y < 60; y++) { //start at row 2 since row 1 is all column titles
                /*
                each new y value is a new step to be created
                x=0: workflow name
                x=1: application id
                x=2: step name
                x=3: step id
                x=4-19: permission set access
                */
                if (excelsheet[2][y].compareTo("[Null] Step") == 0 || excelsheet[0][y].compareTo("[Null] Workflow") == 0){
                    continue;
                }

                //test for unique workflow
                if(p.getworkflowlist().size()<1)
                    p.createworkflow(excelsheet[0][y], excelsheet[1][y]);
                else {
                    boolean unique = p.getworkflowlist().get(p.getworkflowlist().size()-1).getworkflow_name().compareTo(excelsheet[0][y])==0;
                    if(!unique)
                        p.createworkflow(excelsheet[0][y], excelsheet[1][y]);
                }
                p.createstep(excelsheet[0][y], excelsheet[2][y], excelsheet[3][y],excelsheet[stepaccesscounter+4][y]);
            }
        } //finished creating an object for each permission set

        URL url = new URL(app_url + "/api/v1/account/token");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");

        String decoded_key = client_key + ":" + secret_key;
        String encoded_key = Base64.getEncoder().encodeToString(decoded_key.getBytes());
        con.setRequestProperty("Authorization", "Basic " + encoded_key);

        con.setDoOutput(true);
        DataOutputStream out = new DataOutputStream(con.getOutputStream());
        out.flush();
        out.close();

        //Setting Request Headers
        try(BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), "utf-8"))) {
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            System.out.println(response.toString());
            ObjectMapper object = new ObjectMapper();
            token accesskey = object.readValue(response.toString(),token.class);
            bearer_key = accesskey.getAccessToken();
        }

        //Creating Permission Set
        for(permissionset p: permissionsetlist) {
            url = new URL(app_url + "/api/v1/step-permission-sets");
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Authorization", "Bearer " + bearer_key);
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);
            con.setDoInput(true);
            int workflowcounter=0,stepscounter=0;
            String JSON=        "{\n" +
                                "    \"permissions\": [\n";
            workflowcounter=0;
            for(workflow w: p.getworkflowlist()) {
                stepscounter=0;
                workflowcounter++;
                for(step s: w.getsteplist()) {
                    stepscounter++;
                    if (workflowcounter == p.getworkflowlist().size() && stepscounter == w.getsteplist().size()) {
                        JSON += "        {\n" +
                                "            \"operationType\": \"" + s.getStepaccess() + "\",\n" +
                                "            \"step\": {\n" +
                                "                \"id\": \"" + s.getStepid() + "\"\n" +
                                "            },\n" +
                                "            \"type\": \"STEP_PERMISSION\"\n" +
                                "        }\n";
                    }
                    else {
                        JSON += "        {\n" +
                                "            \"operationType\": \"" + s.getStepaccess() + "\",\n" +
                                "            \"step\": {\n" +
                                "                \"id\": \"" + s.getStepid() + "\"\n" +
                                "            },\n" +
                                "            \"type\": \"STEP_PERMISSION\"\n" +
                                "        },\n";
                    }
                }
            }
                JSON +=         "    ],\n" +
                                "    \"name\": \""+ p.getPermissionset_name() +"\",\n" +
                                "    \"application\": {\n" +
                                "        \"id\": \""+ p.getworkflowlist().get(0).getApplication_id() +"\"\n" +
                                "    }\n" +
                                "}";
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = JSON.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                System.out.println(response.toString());
            }
        }

    } // end main

} //end of OpenCSV class

class permissionset {
    private String permissionset_name;
    private List<workflow> workflows = new ArrayList<workflow>();
    public permissionset(String s){
        permissionset_name = s;
    }
    public void createworkflow(String wfname,String wfid){ workflows.add(new workflow(wfname,wfid));}
    public void createstep(String wfname, String step_name,String step_id,String step_access){
        for(workflow w: workflows) {
            if (w.getworkflow_name().compareTo(wfname) == 0)
                w.createstep(step_name,step_id,step_access);
        }
    }
    public List<workflow> getworkflowlist(){ return workflows;}
    public String getPermissionset_name(){ return permissionset_name;}
} //end of permissionset class
class workflow{
    private String workflow_name,application_id;
    private List<step> steps = new ArrayList<step>();
    public workflow(String wfname, String wfid)
    {
        workflow_name = wfname;
        application_id = wfid;
    }
    public String getworkflow_name(){ return workflow_name;}
    public String getApplication_id(){ return  application_id;}
    public void createstep(String name,String id, String access){ steps.add(new step(name,id,access));}
    public List<step> getsteplist(){ return steps;}
} //end of workflow class
class step{
    private String step_name,step_access,step_id;
    public step(String name, String id, String access)
    {
        step_name = name;
        step_id = id;
        step_access = access;
    }
    public String getStepid(){ return step_id;}
    public String getStepaccess(){ return step_access;}
} //end of step class

@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
class token {
    private String accessToken;

    private String tokenType;

    private long expiresIn;

    private String scope;
}
