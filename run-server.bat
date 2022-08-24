docker pull  "rafa606/mjt-ur-server"
docker run -it --network=host  "rafa606/mjt-ur-server" python3 -m services.motionplanner
