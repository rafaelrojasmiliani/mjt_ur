# This file tells docker what image must be created
# in order to be ahble to test this library
FROM ubuntu:20.04

SHELL ["bash", "-c"]

RUN apt-get update &&  DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends -o Dpkg::Options::="--force-confnew" \
                    python3-pip git iputils-ping net-tools netcat screen build-essential lsb-release gnupg2 curl python3-dev gfortran \
        && echo "deb [arch=amd64] http://robotpkg.openrobots.org/packages/debian/pub $(lsb_release -cs) robotpkg" | tee /etc/apt/sources.list.d/robotpkg.list \
        && curl http://robotpkg.openrobots.org/packages/debian/robotpkg.key | apt-key add - \
        && apt-get update \
        && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends -o Dpkg::Options::="--force-confnew" \
                    coinor-libipopt-dev sudo \
                    build-essential pkg-config git \
                    liblapack-dev liblapack3 libopenblas-base libopenblas-dev libgfortran-7-dev python3-tk \
        && pip3 install setuptools matplotlib scipy quadpy six cython sympy\
        && git clone https://github.com/mechmotum/cyipopt.git cyipopt \
        && cd /cyipopt && python3 setup.py build \
        && cd /cyipopt && python3 setup.py install \
        && echo "export PATH=/opt/openrobots/bin:$PATH" >> /etc/bash.bashrc \
        && echo "export PKG_CONFIG_PATH=/opt/openrobots/lib/pkgconfig:$PKG_CONFIG_PATH" >> /etc/bash.bashrc \
        && echo "export LD_LIBRARY_PATH=/opt/openrobots/lib:$LD_LIBRARY_PATH" >> /etc/bash.bashrc \
        && echo "export PYTHONPATH=/opt/openrobots/lib/python3.6/site-packages:$PYTHONPATH" >> /etc/bash.bashrc \
        && echo "export CMAKE_PREFIX_PATH=/opt/openrobots:$CMAKE_PREFIX_PATH" >> /etc/bash.bashrc \
        && mkdir /workspace

COPY modules /workspace/modules
COPY services /workspace/services


WORKDIR /workspace
