# UR tools

This folder contains a set of tools aimed to simplify the development of [Universal Robots' URCaps](https://www.universal-robots.com/it/info-su-universal-robots/centro-notizie/launch-of-urcaps-the-new-platform-for-ur-accessories-and-peripherals/).

Docker image and container handling is based on the `workstation-common` [project](https://gitlab.inf.unibz.it/smartminifactory/workstation-common).


## Structure

directory | description
----------|-------------
`urcap-custom-api` | Custom API tutorial (as a git submodule).
`README.md` | this readme file.
`ur.Dockerfile` | Text document containing all the commands required to assemble the image with the UR tools.
`ur_build_image.sh` | Script to download the URSim and SDK packages and to build the Docker image.
`ur_setup.sh` | Shell[^1] configuration file for handling of the Docker container.

**NOTE**: these tools have been tested only with the URSim version `3.6.1` and UR SDK version `1.3.55`, different versions may require some adjustments on both the `ur_build_image.sh` script and `ur.Dockerfile` file.


## Dependencies

1. `wget` command line tool. Tested with version `1.20.3`.
2. Docker. Tested with version `19.03.7-ce`, build `7141c199a2`.
3. NVIDIA Container Toolkit (i.e., nvidia-docker). Tested with version `2.2.2`.

**NOTE**: the NVIDIA Container Toolkit is not mandatory, but an alternative installation for the OpenGL suite inside the container would be required to make URSim work.


## Building the Docker image

It is enough to execute the command:

```
./ur_build_image.sh
```

This will:

1. download the URSim package,
2. download the SDK package,
3. download the URCaps tutorial in `pdf` format (both HTML and Swing based design),
4. trigger the generation of the Docker image based on the provided Dockerfile.

If no errors will be encountered during the execution there should be three new files on the directory:

```
$ git status
On branch manuel/mjt-ur-client-upgrade
Your branch is up to date with 'origin/master'.

Untracked files:
  (use "git add <file>..." to include in what will be committed)
	URSim_Linux-3.6.1.tar.gz
	sdk-1.3.55.zip
	urcap_tutorial_html.pdf
```

and a new Docker image on the system, based on the `ur.Dockerfile` recipe and named as `ur-${USER}-image`. For example, for a `USER=demo` one has:

```
$ docker image ls
REPOSITORY           TAG                      IMAGE ID            CREATED             SIZE
ur-demo-image     latest                   44f834e16501        2 days ago          3.75GB
```

*Note*: A working Internet network connection is required to download the packages and tutorial.

### Build process customization

The URsim version can be set [here](https://gitlab.inf.unibz.it/smartminifactory/apps/-/blob/master/external/ur/ur_build_image.sh#L91):
Note that modifying the URSim version may require some different rules [here](https://gitlab.inf.unibz.it/smartminifactory/apps/-/blob/master/external/ur/ur_build_image.sh#L24-34).

The SDK version can be set [here](https://gitlab.inf.unibz.it/smartminifactory/apps/-/blob/master/external/ur/ur_build_image.sh#L94).
Note that modifying the SDK version may require some different rules [here](https://gitlab.inf.unibz.it/smartminifactory/apps/-/blob/master/external/ur/ur_build_image.sh#L59-65)

The company group id can be set [here](https://gitlab.inf.unibz.it/smartminifactory/apps/-/blob/master/external/ur/ur_build_image.sh#L118).


## Handling the Docker container

It is required to source the setup script:

```
source ./ur_setup.sh
```

and after that, the following shell methods will be available:

function | description
----------|-------------
`ur_clean` | To remove the user's UR exited container and any dangling image on the system (use with care).
`ur_stop` | To stop the user's UR container; the container is automatically deleted after it is being stopped (use with care).
`ur_start` | To start the user's UR container; the container is automatically created if doesn't exists.
`ur_exec [input_cmd]` | To execute a specific command inside the container, e.g., `ur_exec ls`.
`ur_enter` | To execute an interactive  shell inside the container. it is equivalent to `ur_exec bash`. if the the underlying image has been updated while the container was running it will prompt the user to either update or not the container (answer with care).
`ur_reborn` | To recreate the container and to imeditally execute an interactive shell on it. it is useful when the underlying image has been updated while the container was running (use with care).

**NOTE**: deleting the container may case loss of data, please use it with care (the container is automatically deleted when using `ur_stop` and `ur_reborn`, also it can be deleted when using `ur_star`, `ur_exec` or `ur_enter` after the image has been updated while the container was running).


## Handling URCaps
Inside the container some shell wrapper methods are available for simplifying the URCaps handling:

function | description
----------|-------------
`urcap_new` | Wrapper to the `./newURCap.sh` script provided by the UR SDK.
`urcap_install_remote` | To install a given URCap into the robot. the command need to be executed inside the URCap pacakge and to make it work the remote install rules need to correctly defined on the `pom.xml`.
`urcap_install_sim` | To install a given URCap into the robot. the command need to be executed inside the URCap pacakge and to make it work the ursim install rules need to correctly defined on the `pom.xml` (packages created inside the container already innclude the installation path for URSim).
`ursim_run_urX` | To start URSim, where `X` can be `3`, `5` or `10`, depending in the target robot model.


[^1]: Only `bash` has been tested.
