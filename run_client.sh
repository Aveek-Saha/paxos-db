PROJECT_NETWORK='project4-net'
CLIENT_IMAGE='client-img'
CLIENT_CONTAINER='client-con'

if [ "$1" == "build" ]
then
  echo "----------Cleaning up existing resources----------"
  docker container stop $CLIENT_CONTAINER 2> /dev/null && docker container rm $CLIENT_CONTAINER 2> /dev/null
  docker image rm $CLIENT_IMAGE 2> /dev/null
  
  echo "----------Building images----------"
  docker build -f client.Dockerfile -t $CLIENT_IMAGE --target client-build .
  exit
fi

if [ $# -ne 2 ]
then
  echo "Usage: ./run_client.sh <server-container-name> <port-number>"
  exit
fi

# run client docker container with cmd args
docker run -it --rm --name $CLIENT_CONTAINER \
 --network $PROJECT_NETWORK $CLIENT_IMAGE \
 java -jar //app//client.jar "$1" "$2"

# Manual command 
# docker run -it --rm --name client-con-1 --network project4-net client-img java -jar //app//client.jar server-1 <server-container-name> <port-number>