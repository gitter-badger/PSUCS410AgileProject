// Used code found in the example provided at http://www.codejava.net/java-se/networking/ftp/connect-and-login-to-a-ftp-server
// to connect to an ftp server.
/*
Exit codes
----------
1   IOException on user input for server
2   IOException on user input for username
3   IOException on user input for password
4   User provided invalid arguments when running the program
5   Directory creation failure
6   Failed to connect
*/
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import java.net.InetAddress;
import java.util.Scanner;
import java.io.File;

public class ftp_client {

    private static String server;
    private static String username;
    private static String password;
    private static int port = 21;
    private static FTPClient ftpClient = new FTPClient();

    public static void directSetupArgs(String ser, String use, String pass, int por) {
        server = ser;
        username = use;
        password = pass;
        port = por;
    }
    
    public static void directSetupArgs(connection c) {
        server = c.server;
        username = c.user;
        password = c.getPassword();
        port = c.port;
    }

    private static void showServerReply(FTPClient ftpClient) {
        String[] replies = ftpClient.getReplyStrings();
        if (replies != null && replies.length > 0) {
            for (String aReply : replies) {
                System.out.println("SERVER: " + aReply);
            }
        }
    }

    private static void setupServerUnamePass(String[] args) {
        Scanner input = new Scanner(System.in);
        authentication auth = new authentication();

        if (args.length == 0) {
            // if no arguments were passed in, prompt user for server, username, and password
            // get server
            System.out.print("Server: ");
            server = input.nextLine();
            if (server == null) {
                System.out.println("Critical Failure! Exiting program with exit code 1");
                System.exit(1);
            }
            // get username
            username = auth.getUsername();
            if (username == null) {
                System.out.println("Critical Failure! Exiting program with exit code 2");
                System.exit(2);
            }
            // get password
            password = auth.getPassword();
            if (password == null) {
                System.out.println("Critical Failure! Exiting program with exit code 3");
                System.exit(3);
            }
        } else if (args.length == 1) {
            // User provided server
            server = args[0];
            // get username
            username = auth.getUsername();
            if (username == null) {
                System.out.println("Critical Failure! Exiting program with exit code 2");
                System.exit(2);
            }

            // get password
            password = auth.getPassword();
            if (password == null) {
                System.out.println("Critical Failure! Exiting program with exit code 3");
                System.exit(3);
            }
        } else {
            // Invalid arguments
            System.out.println("Invalid arguments. Only arguments should be server name");
            System.exit(4);
        }
    }

    public static void setupFtp() {
        try {
            if(username.equals("testuser")) {
                port = 3131;
            }
            ftpClient.connect(server, port);
            showServerReply(ftpClient);
            int replyCode = ftpClient.getReplyCode();

            if (!FTPReply.isPositiveCompletion(replyCode)) {
                System.out.println("Operation failed. Server reply code: " + replyCode);
                return;
            }

            boolean success = ftpClient.login(username, password);
            showServerReply(ftpClient);

            if (!success) {
                System.out.println("Could not login to the server");
            } else {
                System.out.println("LOGGED IN SERVER");
            }

        } catch (IOException ex) {
            System.out.println("Oops! Something wrong happened");
            ex.printStackTrace();
            System.exit(6);
        }

    }

    //Creates a new directory on the ftp server
    public static boolean createDirectory(String dirPath) throws IOException {

    boolean exists;
    String root = ftpClient.printWorkingDirectory();
    String[] directories = dirPath.split("/");

    //for each piece of the path, check whether or not the dir exists.  If not, make it.
        for( String dir : directories ) {
        exists = ftpClient.changeWorkingDirectory(dir);

                if(exists) {
            continue;
        }
        else {
            if(!ftpClient.makeDirectory(dir)) {
                            throw new IOException("Failed to create directory " + dirPath + " error=" + ftpClient.getReplyString());
            }
            if(!ftpClient.changeWorkingDirectory(dir)) {
                            throw new IOException("Failed to change to directory " + dirPath + " error=" + ftpClient.getReplyString());
            }

        }
        }
    //change back the current working directory back to where it started.
    ftpClient.changeWorkingDirectory(root);
    return true;
    }



    public static void main(String[] args) {
        // try to load connection information first
        connection myCon = new connection();
        connectionManager myCm = new connectionManager();
        myCm.findUserConnection(myCon);
        if(!myCon.server.equals("")) {
            //System.out.println(myCon.server);
            //System.out.println(myCon.user);
            //System.out.println(myCon.getPassword());
            server = myCon.server;
            username = myCon.user;
            password = myCon.getPassword();
        }
        else {
            // Set up server, username, and password to prepare FTP client
            setupServerUnamePass(args);
        }

        // Set up ftp client with parameters
        setupFtp();

        // grab commands and do stuff for the user
        command_loop(ftpClient);

    }

    private static void command_loop(FTPClient f) {
        Scanner input = new Scanner(System.in);
        connectionManager cm = new connectionManager();

        String commandInput;
        String dirName;
        String getFilePattern = "get \\w.*";
        String getMultipleFilePattern = "getmultiple \\w.*";
        String putFilePattern = "put \\w.*";
        String putMultipleFilePattern = "putmultiple \\w.*";
        String currentDir = System.getProperty("user.dir");
        while(true) {
        try {
                System.out.print("Command: ");
                commandInput = input.nextLine();

            switch (commandInput) {
                case "exit":
                    exit();
                    break;
                case "get address":
                    System.out.println(getRemoteAddress());
                    break;
                case "create dir":
                    System.out.print("Directory name or relative path: ");
                    dirName = input.nextLine();
                    createDirectory(dirName);
                    break;
                case "connection manager":
                case "cm":
                    cm.run();
                    break;
                case "save connection":
                case "cm save":
                    System.out.print("file path: ");
                    dirName = input.nextLine();
                    cm.save(dirName);
                    break;
                case "load connection":
                case "cm load":
                    System.out.print("file path: ");
                    dirName = input.nextLine();
                    cm.load(dirName);
                    break;
                case "change connection":
                case "connection select":
                case "cm select":
                case "cm change":
                    connection c = cm.select();
                    if(c != null) {
                        if(f.isConnected()) {
                          f.disconnect();
                        }
                        directSetupArgs(c);
                        setupFtp(); //reconnect
                    }
                    break;
                case "new connection":
                case "add connection":
                case "cm new":
                case "cm add":
                      cm.add();
                    break;
                case "list local":
                    listLocalFiles(currentDir);
                    break;
                // case "user input": correspondingMethodName();

                default:
                    if (commandInput.matches(getFilePattern)) {
                        try {
                            if (!getFile(commandInput)) {
                                System.out.println("Could not get file from remote server!");
                            }
                        } catch (IOException e) {
                            System.out.println("I/O error getting remote file!");
                        }

                    }
                    
                    if (commandInput.matches(getMultipleFilePattern)) {
                        try {
                            if (!getMultipleFile(commandInput)) {
                                System.out.println("Could not get file(s) from remote server!");
                            }
                        } catch (IOException e) {
                            System.out.println("I/O error getting remote file!");
                        }               	
                    }

                    if (commandInput.matches(putMultipleFilePattern)) {
                        try {
                            if (!putMultipleFile(commandInput)) {
                                System.out.println("Could not put file(s) from remote server!");
                            }
                        } catch (IOException e) {
                            System.out.println("I/O error putting remote file!");
                        }
                    }

                    if (commandInput.matches(putFilePattern)) {
                        try {
                            if (!putFile(commandInput)) {
                                System.out.println("Could not put file(s) from remote server!");
                            }
                        } catch (IOException e) {
                            System.out.println("I/O error putting remote file!");
                        }

                    }

                }
        }
            catch (IOException ex) {
            System.out.println("Oops! Something wrong happened");
            ex.printStackTrace();

            }
        }
    }

    /**
     * Will exit the application with a message
     */
    public static void exit() {
        System.out.println("Thanks for using this FTP client!");
        System.exit(0);
    }

    /**
     * @param f FTPClient
     * @return String
     * Pass in an FTPClient object as an argument
     * return, via a string, the remote address, may return qualified domain name,
     * or ip address, depending on circumstances of the object and machine.
     */
    public static String getRemoteAddress() {
        InetAddress addr = ftpClient.getRemoteAddress();
        return addr.getCanonicalHostName();
    }

    /**
     * @param String
     * @return boolean
     * Pass in input String (may contain multiple words)
     * Get remote file and write it locally as same-named file.
     * If another name is specified after the filename-to-get, write to that name.
     */
    public static boolean getFile(String input) throws IOException {
        boolean retval = false;
        FileOutputStream out;
        String [] splitInput = input.split("\\s+");

        if (splitInput.length > 2) {
            out = new FileOutputStream(splitInput[2]);
        }
        else {
            out = new FileOutputStream(splitInput[1]);
        }

        retval = ftpClient.retrieveFile(splitInput[1], out);
        out.close();

        return retval;
    }

    public static boolean getMultipleFile(String input) throws IOException {
    	boolean retval = false;
        String [] splitInput = input.split("\\s+");
        FileOutputStream out;

        if(splitInput.length > 1) {
            for (int i = 1; i < splitInput.length; ++i) {
                out = new FileOutputStream(splitInput[i]);
                retval = ftpClient.retrieveFile(splitInput[i], out);
                out.close();
                if (!retval) {
                    return false;
                }
            }
        }
    	
    	return retval;
    }
    /**
     * @param String
     * @return boolean
     * Pass in input String (may contain multiple words)
     * Get local file and write it to remote server as same-named file.
     * If another name is specified before the filename-to-get, write to that name.
     */
    public static boolean putFile(String input) throws IOException {
        boolean retval = false;
        String [] splitInput = input.split("\\s+");
        FileInputStream in;

        if (splitInput.length > 2) {
            in = new FileInputStream(splitInput[2]);
        }
        else {
            in = new FileInputStream(splitInput[1]);
        }

        retval = ftpClient.storeFile(splitInput[1], in);
        in.close();

        return retval;
    }

    /**
     * @param String
     * @return boolean
     * @throws IOException
     * pass in a string containing "put multiple ... ... .." with ... being any number of files
     * that you would like to send to the server, in this case you cannot change the filename
     * while in route.
     */

    public static boolean putMultipleFile(String input) throws IOException {
        boolean retval = false;
        String [] splitInput = input.split("\\s+");
        FileInputStream in;

        if(splitInput.length > 1) {
            for (int i = 1; i < splitInput.length; ++i) {
                in = new FileInputStream(splitInput[i]);
                retval = ftpClient.storeFile(splitInput[i], in);
                in.close();
                if (!retval) {
                    return false;
                }
            }
        }

        return true;
    }

    public static boolean listLocalFiles(String userdir) {
        try {
            File folder = new File(userdir);
            File[] fileList = folder.listFiles();
            System.out.println("Current Directory: " + userdir);
            for (int i = 0; i < fileList.length; ++i) {
                if (fileList[i].isFile()) {
                    System.out.println("f: " + fileList[i].getName());
                } else if (fileList[i].isDirectory()) {
                    System.out.println("d: " + fileList[i].getName());
                }
            }
        } catch(Exception e) {return false;}
        return true;
    }
}
