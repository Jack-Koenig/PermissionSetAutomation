# PermissionSetAutomation
Purpose: The purpose of this code is to automate the creation of the permission set matrix

INSTRUCTIONS:
When downloading the code, just download the "Permission Set Automation" folder to your computer. Inside this folder there is a text file called 
"ApplicationInformation.txt" located in the Permission Set Automation/src/ folder. Before running the code update the first four lines in the 
ApplicationInformation.txt document so it contains the necessary information you need.

The first line in the document describes the permission set matrix CSV file location being used to import. 
The second line describes the application URL that you are working in
The third line is the secret key and the fourth line is the client key

The secret and client keys can be found in the RiskCloud's account information (Profile -> Access Key). If you don't have the current secret key (Access 
Key) for this account you can generate a new one by clicking on the "Generate Access Key" button. Copy the Client key and paste it into the Client Key 
section and the Access Key into the Secret Key section in the txt document. 

After updating the text file with the correct information to run the code open your terminal (search for terminal in your applications) and paste this 
line editing your profiles account name in.

java /Downloads/PermissionSetAutomation/Permission\ Set\ Automation/src/PsetAutomation.java 

Once running this you will be prompted to enter the file location of the ApplicationInformation file, paste that and the code will execute.
(ex: /Downloads/PermissionSetAutomation/Permission Set Automation/src/ApplicationInformation.txt)
