# Executive summary

## Assignment overview

My understanding of the purpose and scope of the assignment is as follows. 

We are tasked with creating a distributed server that is fault tolerant using Paxos. Which has specifications as follows.

- Three types of operations should be performed on the server with the following parameters:
    - PUT (key, value) 
    - GET (key) 
    - DELETE (key) 
- These operations should be performed on a key value store.
- Server is multi threaded and can respond to multiple clients at a time. 
- Client and server should communicate using RPC
- Client should pre-populate the KV store with some data, at least 5 of each operation once the database is populated.
- The server is replicated across 5 instances
- Replicas should be fault tolerant.
- Paxos must be used for fault tolerant consensus.
- Replicas must implement Paxos roles likes the Proposers,  Acceptors, and Learners
- Leader election is optional


### Some Client specific requirements:

- CLI args for the client, in the order listed:
    - The hostname or IP address of the server (it must accept either). 
    - The port number of the server.
- Client should have a timeout mechanism and note an unresponsive server in a log message. 
- Client should be resilient to un-requested packets.
- Logs should be timestamped with the current system time in ms precision.
- Client remains mostly unchanged from the last project


### Some Server specific requirements:

- CLI args for the server, in the order listed:
    - the port number of the coordinator
    - the port number the server listens on
- Server should run forever
- Server should print logs of requests received and responses.
- Log should be timestamped with the current system time in ms precision
- Server should be consistent with all it's replicas
- All the different Paxos roles must be implemented
- The Acceptors must be programed to randomly fail at times
- The Acceptors must be able to recover from failures
 

 
## Technical impression

Since this project builds on the previous one, my first step was to figure out what parts of the first assignment could be reused and what parts I could remove since they are no longer needed. Since my code was well structured and I followed the DRY practice (Don't Repeat Yourself) a lot of the existing code could be reused. 

The first step was planning how I wanted the different components of Paxos to fit. I went with the approach where each replica server has a combined Proposer, Acceptor and Learner. Then there was the issue of leader election, which had a straightforward answer. Since the client could send the request to any server, why not make that server the leader for that iteration of the Paxos algorithm. This way there is never any leader conflict.

Then I created the different functions required for the Paxos components. After that I moved on to the crash recovery. I created functions that serialize the various hashmaps maintained by the components as well as the Key value store itself. Once that was done I created a data loader that looks for the serialized files and recovers from a crash by recreating the tables and data that was lost. Finally for the last project requirement I created a way for the acceptors to randomly fail. I did this by returning null randomly during the prepare and accept phases and confirmed that the Paxos algorithm behaved correctly when the proposer doesnt receive a majority vote.


To me the most challenging part of this assignment was figuring out if I should have separate and distinct Paxos roles or integrate all of them into one combined server replica. In the end I went with the latter approach since it simplified the whole design and also ensured maximum resource utilization since there are only as many server replicas as there are proposers, acceptors and learners, compared to dedicated ones.