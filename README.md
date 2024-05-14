# Project 4

There are two folders `client` and `server` containing the client and server respectively. These are two separate Java programs and need to be compiled separately

## Running instructions

The project uses `maven` so to build it on your system you will need maven installed. If you use IntelliJ it should be already installed and you can install the package from the GUI.

The project has one dependency on an external library: `org.JSON`. This jar package needs to be installed using Maven before the code will compile.

Since this Project uses RMI your machine must be able set up for that as well.

This project MUST be run with `Docker` or `Docker Compose` which will create a coordinator and the specified number of server replicas.

### Run with Docker
Docker can be used along with the shell scripts.

```sh
# This script will build and start the coordinators and servers and then display logs from the coordinator
./deploy.sh

# To view logs from any of the 5 servers
docker logs server-<1-5> -f

# Run the client
./run_client.sh server-<1-5> <5001-5005>
```

### Run with Docker Compose
You can also use docker compose to set it up

```sh
# Run the docker compose command
# This will create the network, start the coordinator and the specified number of replicas
docker compose up

# Run: 'docker compose down' when you want to remove the resources created above 

# Build client image
docker build -f client.Dockerfile -t client-img --target client-build .

# Run client container
docker run -it --rm --name client-con-1 --network project4_default client-img java -jar /app/client.jar project4-server-3 5001

# If the above command doesnt work (it didn't work for me on windows git bash) try this one
# docker run -it --rm --name client-con-1 --network project4_default client-img java -jar //app//client.jar project4-server-3 5001
```

If you want to change the number of replicas edit `compose.yaml` and rerun the commands.

```Dockerfile
replicas : 10
```

To change the ports that the servers run on you can also edit `compose.yaml`

```Dockerfile
coordinator:
    ...
    entrypoint: java -jar /app/server.jar c <coordinator port>
    ports:
        - "<coordinator port>:<coordinator port>"
    ...

server:
    ...
    entrypoint: java -jar /app/server.jar coordinator <coordinator port> <server port>
    ports:
        - target: <server port>
    ...
```