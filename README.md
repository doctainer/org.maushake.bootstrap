bootstrap
=========
This util is a java counterpart of the bootstrap.erl erlang application
 see: https://github.com/schlagert/bootstrap

This util allows java jinterface nodes to participate in the process of populating an erlang cluster 
It is also possible to use this util without erlang (and it's **epmd**) to efficiently lookup any number of uniquely named items (nodes)
and make them aware of each other.

The Java API is a Connection Controller which is notified of emerging nodes.
The advantage of using erlangs port mapper daemon (**epmd**) is, that the connection controller gets informed of nodes that go down.


## Build
run   
```
$ mvn package
```
 
## Config
TODO
 
## Run 
The class
`org.maushake.bootstrap.HowTo` (see the test-folder)
shows how to start the bootstrap protocol for a given node. 
Start epmd before invoking this class

### Known Issues
* Transport broadcast is not fully implemented
* A method to stop the protocol is missing
* Epmd-less use needs improvements 