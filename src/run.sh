docker kill $(docker ps -q)
docker build -t slave ./Slave_Labo01
docker build -t master ./Master_Labo01

docker run  -d slave
docker run   master
