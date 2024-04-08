import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatabaseNode {

    private final List<Socket> nodes;
    private final ServerSocket serverNode;
    private Integer key, value;

    /**
     * Creates DatabaseNode object on specified <strong>TCP port</strong> with assigned <strong>Key:Value</strong>
     *
     * @param port  <strong>TCP port</strong> on which this node will be created
     * @param key   variable holding <strong>Key</strong> of this node
     * @param value variable holding <strong>Value</strong> of this node
     */
    public DatabaseNode(int port, int key, int value) throws IOException {
        nodes = new ArrayList<>();
        serverNode = new ServerSocket(port, 0, InetAddress.getByName("localhost"));
        this.key = key;
        this.value = value;
    }

    /**
     * Starts node's processing
     */
    private void launch() {
        while (true) {
            try {
                Socket client = serverNode.accept();
                new NodeThread(client, this).start();
            } catch (IOException e) {
                System.out.println("NO I/0)");
                System.exit(1);
            }
        }
    }

    /**
     * Attaches specified in <strong>nodes</strong> String, nodes to the database
     *
     * @param nodes list of nodes, that should be attached to database in String form
     * @return DatabaseNode object with attached nodes
     */
    private DatabaseNode establishConnection(String nodes) throws IOException {
        if (nodes.isEmpty()) return this;
        String[] flags = nodes.split("\\s");
        for (String flag : flags) {
            String[] hostPort = flag.split(":");
            this.nodes.add(new Socket(hostPort[0], Integer.parseInt(hostPort[1])));
        }
        for (Socket node : this.nodes) {
            PrintWriter out = new PrintWriter(node.getOutputStream(), true);
            out.println("con-" + serverNode.getInetAddress().getHostAddress() + ":" + serverNode.getLocalPort());
        }
        return this;
    }

    private static DatabaseNode createNode(String... args) throws IOException {
        if (args.length == 0) return null;
        int port = 0, key = 0, value = 0;
        String flags = String.join(" ", args);
        StringBuilder connectFlags = new StringBuilder();
        Pattern pattern = Pattern.compile("-(tcpport|record|connect)\\s(\\d+|\\w+):?\\d+");
        Matcher matcher = pattern.matcher(flags);
        while (matcher.find()) {
            String command = matcher.group();
            String[] commandDetails = command.split("\\s");
            switch (commandDetails[0]) {
                case "-tcpport":
                    port = Integer.parseInt(commandDetails[1]);
                    break;
                case "-record":
                    String[] keyValue = commandDetails[1].split(":");
                    key = Integer.parseInt(keyValue[0]);
                    value = Integer.parseInt(keyValue[1]);
                    break;
                case "-connect":
                    connectFlags.append(commandDetails[1]).append(" ");
                    break;
            }
        }
        return new DatabaseNode(port, key, value).establishConnection(connectFlags.toString());
    }

    public String getIPAddress() {
        return serverNode.getInetAddress().getHostAddress();
    }

    public List<Socket> getNodes() {
        return nodes;
    }

    public ServerSocket getServerNode() {
        return serverNode;
    }

    public Integer getKey() {
        return key;
    }

    public void setKey(Integer key) {
        this.key = key;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return getIPAddress() + ":" + serverNode.getLocalPort() + " KV=" + key + ":" + value;
    }

    public static void main(String[] args) {
        try {
            DatabaseNode node = createNode(args);
            if (node == null) {
                System.out.println("Invalid arguments");
                System.exit(1);
            }
            System.out.println(node);
            node.launch();

        } catch (IOException e) {
            System.out.println("No I/O");
            System.exit(1);
        }
    }
}
