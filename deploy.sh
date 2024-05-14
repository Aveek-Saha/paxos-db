PROJECT_NETWORK='project4-net'
SERVER_IMAGE='server-img'
CLIENT_IMAGE='client-img'
CLIENT_CONTAINER='client-con'

COORDINATOR_CONTAINER='coordinator'
SERVER_CONTAINER='server'

# clean up existing resources, if any
echo "----------Cleaning up existing resources----------"
for i in {1..5}
do
  docker container stop $SERVER_CONTAINER-$i 2> /dev/null && docker container rm $SERVER_CONTAINER-$i 2> /dev/null 
done
docker container stop $CLIENT_CONTAINER 2> /dev/null && docker container rm $CLIENT_CONTAINER 2> /dev/null
docker container stop $COORDINATOR_CONTAINER 2> /dev/null && docker container rm $COORDINATOR_CONTAINER 2> /dev/null
docker image rm $SERVER_IMAGE 2> /dev/null
docker image rm $CLIENT_IMAGE 2> /dev/null
docker network rm $PROJECT_NETWORK 2> /dev/null

# only cleanup
if [ "$1" == "clean" ]
then
  exit
fi

# create a custom virtual network
echo "----------creating a virtual network----------"
docker network create $PROJECT_NETWORK

# build the images from Dockerfile
echo "----------Building images----------"
docker build -f client.Dockerfile -t $CLIENT_IMAGE --target client-build .
docker build -f server.Dockerfile -t $SERVER_IMAGE --target server-build .

echo "----------Running coordinator app----------"
docker run -d -p 5000:5000 --name $COORDINATOR_CONTAINER \
  --network $PROJECT_NETWORK $SERVER_IMAGE \
  java -jar //app//server.jar c 5000
# docker run -d -p 5000:5000 --name $COORDINATOR_CONTAINER --network $PROJECT_NETWORK $SERVER_IMAGE java -jar /app/server.jar c 5000

# create 5 server containers
echo "----------Creating server containers----------"
for i in {1..5}
do
  docker run -d -p 500$i:500$i --name $SERVER_CONTAINER-$i \
    --network $PROJECT_NETWORK $SERVER_IMAGE \
    java -jar //app//server.jar $COORDINATOR_CONTAINER 5000 500$i 3
  # docker run -d -p 500$i:500$i --name $SERVER_CONTAINER-$i --network $PROJECT_NETWORK $SERVER_IMAGE java -jar /app/server.jar coordinator 5000 500$i
done

echo "----------watching logs from coordinator----------"
docker logs $COORDINATOR_CONTAINER -f