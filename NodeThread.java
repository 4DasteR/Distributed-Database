import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class NodeThread extends Thread {

    private final Socket socket;
    private final DatabaseNode node;

    /**
     * Creates working process for handling messages to the node
     *
     * @param socket connection to another <strong>node</strong> or <strong>client</strong>
     * @param node   reference to <strong>DatabaseNode</strong> object on which this process will be executed
     */
    public NodeThread(Socket socket, DatabaseNode node) {
        this.socket = socket;
        this.node = node;
    }

    @Override
    public void run() {
        try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String operation = in.readLine();
            if (operation != null) {
                if (!operation.startsWith("con-")) {
                    if (!operation.startsWith("Node ")) {
                        System.out.println("Received operation: " + operation);
                        //Setting up operation alongside list of already checked nodes
                        List<String> checked = new ArrayList<>();
                        String[] opCH = operation.split("\\|");
                        operation = opCH[0];

                        //Filling out list of checked nodes
                        if (opCH.length > 1) checked = generateCheckedNodesList(opCH[1]);
                        checked.add(node.getIPAddress() + ":" + node.getServerNode().getLocalPort());

                        //Handling operation to Executing Unit
                        String response = execute(operation, checked);
                        System.out.println("Response: " + response.replaceAll(";", ""));

                        //Returning response to operation sender
                        out.println(response.replaceAll(";", ""));

                        //Terminating node, when receiving 'terminate' operation's response
                        if (response.startsWith(";")) {
                            socket.close();
                            System.exit(0);
                        }
                    } else {
                        //Handling detaching terminated node from this node
                        System.out.println(operation + ".");
                        String[] closedNodeInfo = operation.replaceAll("Node\\s|\\sterminated", "").split(":");
                        node.getNodes().removeIf(s -> s.getInetAddress().getHostAddress().equals(closedNodeInfo[0]) &&
                                s.getPort() == Integer.parseInt(closedNodeInfo[1]));
                    }
                } else {
                    //Attaching another node to distributed database
                    operation = operation.replaceAll("con-", "");
                    System.out.println("Node " + operation + " has connected.");
                    String[] hostPort = operation.split(":");
                    node.getNodes().add(new Socket(hostPort[0], Integer.parseInt(hostPort[1])));
                }

            }
        } catch (IOException e) {
            System.out.println("NO I/O");
            System.exit(1);
        }
    }

    /**
     * @param checkedList list of nodes that were already checked inside the database for specified operation in String form
     * @return list of nodes that were already checked inside the database for specified operation
     */
    private synchronized List<String> generateCheckedNodesList(String checkedList) {
        return Arrays.stream(checkedList.split("\\s")).collect(Collectors.toList());
    }

    /**
     * @param mode      mode for the function: MAX if desired key:value has maximal value in the database, otherwise key:value will be returned for minimal value in database
     * @param operation operation to be performed for the database
     * @param checked   list of nodes that were already checked inside the database for specified operation
     * @return key and value of a node fitting specified mode
     */
    private synchronized String getMinOrMax(String mode, String operation, List<String> checked) throws IOException {
        int desiredValue = node.getValue();
        String allPairs = node.getKey() + ":" + node.getValue() + " ";
        allPairs += callConnectedNodes(operation, checked);
        allPairs = allPairs.replaceAll("\\sERROR\\s*", "");
        for (String pair : allPairs.split("\\s")) {
            int val = Integer.parseInt(pair.split(":")[1]);
            desiredValue = (mode.equalsIgnoreCase("MAX")) ? Math.max(desiredValue, val) : Math.min(desiredValue, val);
        }
        for (String pair : allPairs.split("\\s")) {
            int val = Integer.parseInt(pair.split(":")[1]);
            if (val == desiredValue) return pair;
        }
        return node.getKey() + ":" + node.getValue();
    }

    /**
     * @param operation operation to be performed for the database
     * @param checked   list of nodes that were already checked inside the database for specified operation
     * @return result of specified operation or ERROR
     */
    private synchronized String callConnectedNodes(String operation, List<String> checked) throws IOException {
        for (Socket connectedNode : node.getNodes()) {
            if (checked.stream().anyMatch(s -> s.equals(connectedNode.getInetAddress().getHostAddress() + ":" + connectedNode.getPort())))
                continue;

            Socket nSocket = new Socket(connectedNode.getInetAddress().getHostAddress(), connectedNode.getPort());
            PrintWriter out = new PrintWriter(nSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(nSocket.getInputStream()));

            //Adding checked nodes to operation
            StringBuilder sb = new StringBuilder(operation);
            if (!operation.contains("|")) sb.append("|");
            for (String checkedNode : checked) if (!operation.contains(checkedNode)) sb.append(checkedNode).append(" ");

            operation = sb.toString();

            out.println(operation);
            String response = in.readLine();
            in.close();
            out.close();
            nSocket.close();
            if (response == null || response.equals("ERROR")) continue;
            return response;
        }
        return "ERROR";
    }

    /**
     * Executes operation for <strong>DatabaseNode</strong> for which this process was created
     *
     * @param operation operation to be performed for the database starting with this node
     * @param checked   list of nodes that were already checked inside the database for specified operation
     * @return result of specified operation or ERROR
     */
    private synchronized String execute(String operation, List<String> checked) throws IOException {
        String[] operationParameter = operation.split("\\s");
        int searchKey, nValue;
        switch (operationParameter[0]) {

            case "terminate":
                node.getNodes().forEach(s -> {
                    try (Socket nSocket = new Socket(s.getInetAddress().getHostAddress(), s.getPort())) {
                        PrintWriter out = new PrintWriter(nSocket.getOutputStream(), true);
                        out.println("Node " + node.getIPAddress() + ":" + node.getServerNode().getLocalPort() + " terminated");
                        out.close();
                        s.close();
                    } catch (IOException e) {
                        System.out.println("NO IO terminate");
                        System.exit(1);
                    }
                });
                return ";OK";

            case "get-value":
                searchKey = Integer.parseInt(operationParameter[1]);

                if (node.getKey() == searchKey) return node.getKey() + ":" + node.getValue();
                else return callConnectedNodes(operation, checked);

            case "set-value":
                String[] keyValue = operationParameter[1].split(":");
                searchKey = Integer.parseInt(keyValue[0]);
                nValue = Integer.parseInt(keyValue[1]);

                if (node.getKey() == searchKey) {
                    node.setValue(nValue);
                    return "OK";
                } else return callConnectedNodes(operation, checked);

            case "find-key":
                searchKey = Integer.parseInt(operationParameter[1]);

                if (node.getKey() == searchKey)
                    return node.getServerNode().getInetAddress().getHostAddress() + ":" + node.getServerNode().getLocalPort();
                else return callConnectedNodes(operation, checked);

            case "new-record":
                keyValue = operationParameter[1].split(":");
                searchKey = Integer.parseInt(keyValue[0]);
                nValue = Integer.parseInt(keyValue[1]);

                node.setKey(searchKey);
                node.setValue(nValue);
                return "OK";
            case "get-max":
                return getMinOrMax("max", operation, checked);
            case "get-min":
                return getMinOrMax("min", operation, checked);
        }
        return "ERROR";
    }
}
