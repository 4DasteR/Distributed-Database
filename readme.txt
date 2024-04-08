This project creates and allows to operate on a distributed database, via creation of nodes and performing varoius operations (described further in text) via client.

Chapter 1. Creation of network:
    Main component of the network is node, which holds an integer key and associated with it integer value.
Node not only consits of key, value pair but also of java.net.ServerSocket, which allows other nodes and clients to connect to it via client.
Additionally every node has a list of other nodes to which this node is connected.
Each node is defined by ip address (in project it's localhost for every node) and tcp port number, which will be used for further comunication between other nodes and clients.


Chapter 2. Connection between nodes:
    Upon creation of node, list of connected nodes is created, which by default is empty.
When another node is created and it has `-connect <ip address>:<port>` flag, it will create a TCP socket for ip address and port specified in earlier mentioned flag and add it to list of other nodes.
After creation of said socket, it will send a message in form `con-<ip address>:<port>`, where ip address and port are parameters of connecting node.
Firstly created node upon recieving message `con-<ip address>:<port>` will add to its own nodes' list new TCP socket for ip address and port specified in recieved message.
Following process ensures establishing of two-way connection between nodes.


Chapter 3. Communication in application:
    Communication in application is based on sending single line message ended with end line character.
    Message takes format of ASCII (String) `<operation>[| [checked ip addresses]] ended with new line character. 
After recieving operation if current node doesn't fit required in `<operation>` criteria it will add `|<ip address>:<port> `, and forward altered operation to the nodes connected to it.
Character '|' is added only once and indicates beginning of the list of already checked nodes in form of `<ip address>:<port>` separated from each other by spaces.

Node's operating cycle:
    1. Creation of node.
        - Optionally if said node connects to the other node it will follow procedure described in chapter 2 - "Connection between nodes"
    2. Waiting for clients.
    3. Recieving message from client.
    4. Executing operations, described in chapter 4 - "Operations on nodes and their implementation":
        a. If operation isn't successful sending message to all connected nodes in accordance with proper form described at the beginning of this chapter.
        b. If connected node responds with a message other than 'ERROR' or 'null' it will be forwarded as a response to the originally recieving node, otherwise response will be 'ERROR'.
    5. Sending proper response to the client.
    6. Ending communication process with client.
    7. If recieved operation was `terminate`, node will send message to other connected to it nodes that it terminates and will detach itself from distributed database, otherwise go back to step 2.


Chapter 4. Operations on nodes and their implementation:
    There are 7 operations, which by default are sent by client and one operation for connecting between nodes.

    After recieving message, if message doesn't contain "con-" or "Node ", it will be split by character '|' and left part which is the exact operation will be asigned to string variable, while right part of message will be partitioned by space and each of `<ip address>:<port>` string will be saved to a list named "checked".
Current node's ip address and pair will be added to checked in form `<ip address>:<port>`.
Next node will call "execute" method witch parameters "operation" and "checked".
Inside "execute" method "operation" will be split into two parts, where first one is command and second command's value into operationParameter variable.
Then switch based on first part of operationParameter will determine according implementation of method.

Operations:
    1. `terminate` - this operation terminates node to which it was sent. Before terminating the node will notify nodes connected to it that it will be terminating, and they have to disconnect from it.
        Similary, before terminating this node will disconnect itself from other connected nodes. Afterwards it will send ;OK message (which will be displayed as 'OK'), character ';' is used to notify node that it has to disable itself.
        Implementation:
            Using for each loop current node will go over all of it's connected nodes and send them a message in form "Node `<ip address>:<port>` terminated". Then it will close the socket that connected to other node.
            After loop "execute" method will return ";OK" message.
            If response message contains ';', it will be displayed without this character.
            Node will check if response starts with ';', and if it starts it will disable itself, by using System.exit(0).

    2. `get-value <key>` - this operation returns `<key>:<value>` pair according to searched key, which is obtained from second part of operationParameter variable. If searched key is found, it will be returned alongside its dedicated value in `<key>:<value>` pair. 
        If searched key is not found nodes will return "ERROR".
        Implementation:
            Searched key will be parsed into Integer from second part of operationParameter.
            If searched key matches current node's key the `<key>:<value>` pair will be returned.
            If searched key cannot be found the operation, the operation will be forwarded to connected nodes using "callConnectedNodes" method, described in chapter 5 - "Auxiliary methods".
            If desired key was found in connected nodes, the response will be returned to the current node in specified form.
            If not a string "ERROR" will be returned.

    3. `set-value <key>:<value>` - this operation changed the value for desired key into desired value. If operation was successful node will return "OK", otherwise "ERROR".
        Implementation:
            Searched key and desired new value will be parsed into Integer from second part of operationParameter split by ':'.
            If searched key matches current node's key the value of that node will be replaced with new one.
            If searched key cannot be found the operation, the operation will be forwarded to connected nodes using "callConnectedNodes" method, described in chapter 5 - "Auxiliary methods".
            If desired key was found in connected nodes, the value of that node will be replaced with new one and "OK" will be returned.
            If not a string "ERROR" will be returned.

    4. `find-key <key>` - this operation searches for node holding specified key. If node with desired key was found, the ip address and port will be returned in `<ip address>:<port>`, otherwise "ERROR".
        Implementation:
            Searched key will be parsed into Integer from second part of operationParameter.
            If searched key matches current node's key the `<ip address>:<port>` pair will be returned.
            If searched key cannot be found the operation, the operation will be forwarded to connected nodes using "callConnectedNodes" method, described in chapter 5 - "Auxiliary methods".
            If desired key was found in connected nodes, the response will be returned to the current node in specified form.
            If not a string "ERROR" will be returned.

    5. `get-max` - this operation searches for highest value among all nodes in distributed database that are in some way connected to each other. The maximal value will be returned alongside its key in form of `<key>:<value>` pair.
        Implementation:
            Node will return result of method "getMinOrMax", with parameters "max", operation and checked.
            This method is described in chapter 5 - "Auxiliary methods".

    6. `get-min` - this operation searches for lowest value among all nodes in distributed database that are in some way connected to each other. The maximal value will be returned alongside its key in form of `<key>:<value>` pair.
        Implementation:
            Node will return result of method "getMinOrMax", with parameters "min", operation and checked.
            This method is described in chapter 5 - "Auxiliary methods".

    7. `new-record <key>:<value>` - this operation repleces current `<key>:<value>` pair, with specified inside the operation. After performing operation "OK" will be returned";
        Implementation:
            Desired new key and new value will be parsed into Integer from second part of operationParameter split by ':'.
            Current key will be changed into new key, and current value will be changed into new value.
    
    * `con-<ip address>:<port>` - this is a special operation done by the node to which connects to other node. 
        When current node recieves this command it will establish connection to the node with specified in operation ip address and port.
        Implementation:
            - Said operation occurs before "execute" method.
            String "con-" will be erased from message and a message will be displayed on node's console: "Node <ip address>:<port> has connected.".
            `<ip address>:<port>` will be assigned to string array hostPort split by ':'.
            Current node will add to its list of nodes socket connected to first part of hostPort (ip address) and parsed Integer from second part of hostPort (port).
    
Chapter 5. Auxiliary methods:
    In order to optimise program, some repeatable parts of code were packed into two different methods.
Methods:
    1. "callConnectedNodes(String operation, List<String> checked)" - this method sends operation to the nodes connected to current node, only if connected node doesn't match nodes inside list checked.
        This method will return response in accordance with specified operation.
        If none of connected methods return response different from "ERROR" or null, method will return "ERROR".
        Implementation:
            Start of for each loop interating over all nodes connected to current node.
            If pair of ip address and port of connected node matches those inside list checked, the loop will omit that node and move onto other one using continue.
            Temporary new socket will be created with connection to said node alongside its java.io.PrintWriter and java.io.InputStreamReader.
            StringBuilder object will be created with operation variable as its base string.
            If operation doesn't contain '|' it will be added (ultimately character '|' is added only once as specified in chapter 3 - "Communication in application") to StringBuilder.
            For each string inside checked list, if operation message doesn't contain string from current iteration of loop it will be added with space at the end to StringBuilder.
            Original operation variable will be overriden with the one from StringBuilder.
            PrintWriter will send this operation message to the connected node, which is currently itereated using temporary socket.
            After recieving response, temporary socket and it's all components will be closed.
            If response is  null or "ERROR" the current iteration will be skipped using continue.
            Otherwise the response will be returned, and the method will end.
            If no response was returned and the for each loop has ended, "ERROR" will be returned.
    
    2. "getMinOrMax(String mode, String operation, List<String> checked)" - this operation returnes either the highest or the lowest `<key>:<value>` pair from all nodes that are in some way connected inside distributed database depending on specified mode.
        Implementation:
            Value of current node will be assigned to int variable desiredValue.
            String allPairs will be created with content equal to `<key>:<value>` of node executing the method.
            String allPairs will concatenate result of "callConnectedNodes" method for the same operation.
            String allPairs will have erased part matching regex \sERROR\s*.
            Start of for each loop for String pair inside allPairs split by space.
            Value of Integer parsed from second part of pair split by ':' will be assigned to variable val.
            If mode parameters equals (equalsIgnoreCase()) "MAX", then desiredValue will be set to higher value from desiredValue or val.
            Otherwise desiredValue will be set to lower value from desiredValue or val.
            After iterating over all pairs, another for each loop for String pair inside allPairs split by space will begin.
            Value of Integer parsed from second part of pair split by ':' will be assigned to variable val.
            If val equals desiredValue then pair of `<key>:<value>` will be returned, and the method will end.
            After iterating over all pairs if no pair was returned return pair of `<key>:<value>` of node executing the method.
    
    * "generateCheckedNodesList(String checkedList)" - additional method executed once before "execute" method, which will split second part of original message by space and add each String of `<ip address>:<port>` to list of strings and then return that list.

Chapter 6. Compilation
    In order to compile .java files user should use "compile.bat" file which is located inside same folder as this document.
If user wants to compile files manually, he should open his operating system's terminal/console and write following line:
'javac -source 8 -encoding utf8 *.java'
After writing the line user should hit 'Enter' on the keyboard.