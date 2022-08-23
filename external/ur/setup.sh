#!/usr/bin/env bash

# docker container handling inspired from:
#   https://gitlab.inf.unibz.it/smartminifactory/workstation-common

whereami=$(grep docker /proc/1/cgroup)
if [ ! -z "$whereami" ]; then
  # script not loaded inside containers
  return
fi

export myimage="ur-${USER}-image:latest"
export myhost="ur-${USER}-container"
export mygroup="$(id -g -n ${USER})"

function ur_clean () {
  nvidia-docker ps -a |awk '/ur-.*-image.*Exited/ { print $1 }'|xargs nvidia-docker rm
  nvidia-docker image prune
}

function ur_stop() {
  nvidia-docker stop "${myhost}"
  nvidia-docker rm "${myhost}"
}

function ur_start() {
  # grab the container status
  container=$(nvidia-docker inspect -f '{{.State.Pid}}' ${myhost} 2>/dev/null)
  code=$?
  run=0

  # does the container need to be created?
  if [ "$code" -ne "0" ]; then
    # create it based on the latest image available
    run=1
  else
    # verify its image version
    latest=$(nvidia-docker inspect -f "{{.Id}}" ${myimage%:*})
    current=$(nvidia-docker inspect -f "{{.Image}}" ${myhost})
    if [ "$latest" != "$current" ]; then
      echo "a new image version has been released."
      echo "do you wish to update the container image?"
      echo "WARNING: all running processes will be killed"
      select answer in "Yes" "No"; do
        case $answer in
          Yes )
            # update it based on the latest image available
            ur_stop
            run=1
            break;;
          No )
            break;;
        esac
      done
    fi
  fi

  # need to create the container?
  if [ "$run" -eq "1" ]; then
    nvidia-docker run -d -it \
      -v "/tmp/.X11-unix:/tmp/.X11-unix:rw" \
      -v "/home/${USER}/.ssh:/home/${USER}/.ssh:ro" \
      --env="USER" --env="DISPLAY" \
      --user="${USER}:${mygroup}" -h "${myhost}" \
      --name "${myhost}" \
      ${myimage} bin/bash

    # get container pid
    container=$(nvidia-docker inspect -f '{{.State.Pid}}' "${myhost}" 2>/dev/null)
  fi

  # does the container have been stopped?
  if [ "$container" -eq "0" ]; then
    nvidia-docker start "${myhost}"
  fi
}

function ur_exec () {
  # validate input arguments
  if [ "$#" -eq 0 ]; then
    echo "usage: ur_exec cmd"
  else
    # initialize or update the container (if required/desired)
    ur_start

    # spawn a new process inside the container
    nvidia-docker exec --user="$USER:$mygroup" --workdir "/home/$USER" -it "${myhost}" "$@"
  fi
}

function ur_enter() {
  ur_exec bash
}

function ur_reborn() {
  ur_stop
  ur_enter
}


function ur_mvn() {
    nvidia-docker run --volume $(pwd):/test --user $(id -u):$(id -g) ${myimage} /bin/bash -c "cd test/ && mvn install"
}

# allow to execute graphical applications inside containers
if [ -n "$SSH_CLIENT" ]; then  # ssh connection ...
  if [ -z "$DISPLAY" ]; then   # ... without X forwarding
    export DISPLAY=$(who|awk '$2 ~ /tty/ { print $5 }'|tr -d '()')
  fi
fi

# allow container to connect the X server
xhost +local:root >/dev/null

# export functions only outside container
export -f ur_clean
export -f ur_stop
export -f ur_start
export -f ur_exec
export -f ur_enter
export -f ur_reborn
