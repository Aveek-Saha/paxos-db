# Requirements

## Main

- Three types of operations should be performed on the server:
    - PUT (key, value)
    - GET (key)
    - DELETE (key)
- Use a hash map as a kv store
- Server is replicated across 5 instances
- Clients should be able to connect to any of the replicas
- Replicas should be fault tolerant.
- Paxos must be used for fault tolerant consensus.
- Replicas must implement Paxos roles likes the Proposers,  Acceptors, and Learners
- Leader election is optional

## Other

- Client should pre-populate the KV store with some data, at least 5 of each operations once the database is populated.

# Client req

- CLI args for the client, in the order listed:
    - The hostname or IP address of the server (it must accept either).
    - The port number of the server.
- Client should have a timeout mechanism and note an unresponsive server in a log message.
- Log should be timestamped with the current system time in ms precision.

# Server req

- CLI args for the server, in the order listed:
    - the port number of the coordinator
    - the port number to listen on
- Server should run forever
- Server should print logs of requests received and responses.
- Log should be timestamped with the current system time in ms precision.
- Server should be multithreaded
- Mutual exclusion should be handled
- Server should be consistent with all it's replicas
