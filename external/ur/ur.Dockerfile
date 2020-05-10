# docker image file structure inspired from:
#   https://gitlab.inf.unibz.it/smartminifactory/workstation-common
# summary of installation and URCap generation:
#  https://github.com/KalervoHyyppa/URSim-and-URCap-Native-Linux-Installation-Guide
FROM nvidia/cudagl:10.0-devel-ubuntu16.04

# base packages
RUN echo "deb http://ppa.launchpad.net/apt-fast/stable/ubuntu bionic main" >> /etc/apt/sources.list || exit 1; \
    echo "deb-src http://ppa.launchpad.net/apt-fast/stable/ubuntu bionic main" >> /etc/apt/sources.list || exit 1; \
    apt-key adv --keyserver keyserver.ubuntu.com --recv-keys A2166B8DE8BDC3367D1901C11EE2FF37CA8DA16B || exit 1
RUN apt-get update || exit 1
RUN DEBIAN_FRONTEND=noninteractive apt-get upgrade -y --no-install-recommends -o Dpkg::Options::="--force-confnew" || exit 1
RUN DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends -o Dpkg::Options::="--force-confnew" \
    apt-utils apt-fast || exit 1; \
    echo debconf apt-fast/maxdownloads string 5 | debconf-set-selections || exit 1; \
    echo debconf apt-fast/dlflag boolean true | debconf-set-selections || exit 1; \
    echo debconf apt-fast/aptmanager string apt-get | debconf-set-selections || exit 1
RUN DEBIAN_FRONTEND=noninteractive apt-fast install -y --no-install-recommends -o Dpkg::Options::="--force-confnew" \
    dh-make fakeroot build-essential pkg-config devscripts lsb-release gdb bash-completion command-not-found || exit 1
RUN DEBIAN_FRONTEND=noninteractive apt-fast install -y --no-install-recommends -o Dpkg::Options::="--force-confnew" \
    sudo psmisc openssh-client nmap netcat-openbsd wget git vim screen unrar unzip ccache less locales || exit 1
RUN DEBIAN_FRONTEND=noninteractive apt-fast install -y --no-install-recommends -o Dpkg::Options::="--force-confnew" \
    python3-dev python3-pip python3-setuptools python3-argcomplete || exit 1

# UR tools dependencies
RUN DEBIAN_FRONTEND=noninteractive apt-fast install -y --no-install-recommends -o Dpkg::Options::="--force-confnew" \
    default-jdk default-jre maven dialog sshpass || exit 1

# bash and python3 tab completion
RUN echo $'\n\
if [ -f /usr/share/bash-completion/bash_completion ]; then\n\
  . /usr/share/bash-completion/bash_completion\n\
elif [ -f /etc/bash_completion ]; then\n\
  . /etc/bash_completion\n\
fi\n' >> /etc/bash.bashrc
RUN activate-global-python-argcomplete3 || exit 1

# set the locale
RUN locale-gen en_US.UTF-8
ENV LANG=en_US.UTF-8 LANGUAGE=en_US:en LC_ALL=en_US.UTF-8

# add non root user
ARG myuser
ARG myuid
ARG mygroup
ARG mygid
RUN groupmod -g ${mygid} ${mygroup}
RUN adduser --gecos "" --disabled-password --uid ${myuid} --gid ${mygid} ${myuser}
RUN echo "${myuser} ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers

# copy and install ursim
ARG mysim
COPY ./${mysim} /opt/${mysim}
RUN cd /opt/${mysim} && ./install.sh || exit 1

# copy and install the sdk
ARG mysdk
COPY ./${mysdk} /opt/${mysdk}
RUN cd /opt/${mysdk} && echo "n"|sudo -u ${myuser} ./install.sh || exit 1

# some aliases for ursim and urcaps handling
ARG mycompany
RUN echo "alias urcap_new='/opt/${mysdk}/newURCap.sh'" >> /home/${myuser}/.bashrc; \
    echo "alias urcap_install_sim='mvn install -Pursim'" >> /home/${myuser}/.bashrc; \
    echo "alias urcap_install_robot='mvn install -Premote'" >> /home/${myuser}/.bashrc; \
    echo "alias ursim_run_ur3='/opt/${mysim}/start-ursim.sh UR3'" >> /home/${myuser}/.bashrc; \
    echo "alias ursim_run_ur5='/opt/${mysim}/start-ursim.sh UR5'" >> /home/${myuser}/.bashrc; \
    echo "alias ursim_run_ur10='/opt/${mysim}/start-ursim.sh UR10'" >> /home/${myuser}/.bashrc; \
    sudo -i -u ${myuser} git config --global alias.dpull '! git pull && git submodule update --init --recursive'; \
    sudo -i -u ${myuser} git config --global alias.pfetch 'fetch --prune --all'; \
    sed -i -e's/\(mygroupid=\)".*"/\1'${mycompany}'/g' /opt/${mysdk}/newURCap.sh; \
    echo "sed -i -e's#\(<ursim.home>\)\(</ursim.home>\)#"'\\1'"/opt/${mysim}"'\\2'"#g' \${mypackage}/pom.xml" >> /opt/${mysdk}/newURCap.sh

# update permissions
RUN chown -LR ${myuser}:${mygroup} /home/${myuser} /opt/*

# apt-get and /tmp clean-up
RUN rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/* /tmp/*
