#!/usr/bin/env bash

function setup_ur_sim() {
  if [ "$#" -ne 1 ]; then
    echo "input arguments error."
    echo ">> usage: download_ur_sim target_ur_sim"
    return 1
  fi

  local sim_path="URSim_Linux-${1}"
  local sim_file="${sim_path}.tar.gz"

  if ! wget -c "https://s3-eu-west-1.amazonaws.com/ur-support-site/66364/${sim_file}" &>/dev/null; then
    echo "download error."
    return 1
  fi

  sim_path=$(tar -tzvf "${sim_file}"|sed -n -e'0,/ursim/ s#.*\(\./ursim[^/]*\)/.*#\1#p')
  if ! tar -xzvf "${sim_file}" &>/dev/null; then
    echo "decompression error."
    return 1
  fi

  local sim_install="${sim_path}/install.sh"
  sed -i -e's/tty -s/true/g' "${sim_install}"
  sed -i -e's/sudo *//g' "${sim_install}"
  sed -i -e's/pkexec bash -c "\(.*\)"/\1/g' "${sim_install}"
  sed -i -e's#FILE=.*\($TYPE.*\)#FILE=/tmp/ursim-\1#g' "${sim_install}"
  sed -i -e's#\.\./URControl#\./URControl#g' "${sim_install}"
  sed -i -e's/pushd.*//g' "${sim_install}"
  sed -i -e's/popd.*//g' "${sim_install}"
  sed -i -e's/dpkg/sudo dpkg/g' "${sim_install}"
  sed -i -e's/'\''$packages'\''/"$packages"/g' "${sim_install}"
  sed -i -e's/apt-get -y install \(.*\)/DEBIAN_FRONTEND=noninteractive apt-fast install -y --no-install-recommends -o Dpkg::Options::="--force-confnew" \1 || exit 1/g' "${sim_install}"

  echo ${sim_path}
}

function setup_ur_sdk() {
  if [ "$#" -ne 1 ]; then
    echo "arguments error."
    echo ">> usage: download_ur_sdk sdk_version"
    return 1
  fi

  local sdk_path="sdk-${1}"
  local sdk_file="${sdk_path}.zip"

  if ! wget -c "https://s3-eu-west-1.amazonaws.com/urplus-developer-site/sdk/${sdk_file}" &>/dev/null; then
    echo "download error."
    return 1
  fi

  if ! unzip -od "./${sdk_path}" "${sdk_file}" &>/dev/null; then
    echo "decompression error."
    return 1
  fi

  local sdk_install="${sdk_path}/install.sh"
  sed -i -e's/exit/exit 1/g' "${sdk_install}"
  sed -i -e's/^\(mvn.*\)$/\1 || exit 1/g' "${sdk_install}"
  sed -i -e's/\(sudo apt-get update\)/\1 || exit 1/g' "${sdk_install}"
  sed -i -e's/\(sudo apt-\)get\( install -y\) \(.*\)/DEBIAN_FRONTEND=noninteractive \1fast\2 --no-install-recommends -o Dpkg::Options::="--force-confnew" \3 || exit 1/g' "${sdk_install}"
  sed -i -e's/dpkg -i/dpkg --force-overwrite -i/g' "${sdk_install}"
  sed -i -e'/dpkg --/s/$/;\n\tDEBIAN_FRONTEND=noninteractive apt-fast install -f -y --no-install-recommends -o Dpkg::Options::="--force-confnew" || exit 1/g' "${sdk_install}"

  echo "${sdk_path}"
}

function build_ur_image() {
  if [ "$#" -ne 3 ]; then
    echo "usage: build_ur_image path_to_ur_sdk path_to_ur_sim company_id"
    return 1
  fi

  myuid=$(id -u "${USER}")
  mygid=$(id -g "${USER}")
  mygroup=$(id -g -n "${USER}")
  nvidia-docker build -t "ur-${USER}-image" \
    --build-arg myuser="${USER}" \
    --build-arg myuid="${myuid}" \
    --build-arg mygroup="${mygroup}" \
    --build-arg mygid="${mygid}" \
    --build-arg mysim="${1}" \
    --build-arg mysdk="${2}" \
    --build-arg mycompany="${3}" \
    -f ur.Dockerfile .
}

function setup_ur_tools () {
  local ur_sim_version="3.12.1.90940"
  local ur_sim_path

  local ur_sdk_version="1.9.0"
  local ur_sdk_path

  echo -n "retrieving UR SIM... "
  if ! ur_sim_path=$(setup_ur_sim "${ur_sim_version}"); then
    echo "${ur_sim_path}"
    exit 1
  fi
  echo "done!"

  echo -n "retrieving UR SDK... "
  if ! ur_sdk_path=$(setup_ur_sdk "${ur_sdk_version}"); then
    echo "${ur_sdk_path}"
    exit 1
  fi
  echo "done!"

  echo -n "downloading /URCaps tutorial HTML..."
  if ! wget -c https://plus.universal-robots.com/media/1810566/urcap_tutorial_html.pdf &>/dev/null; then
    echo "download error."
    return 1
  fi
  echo "done!"

  echo -n "downloading /URCaps tutorial Swing..."
  if ! wget -c https://plus.universal-robots.com/media/1810567/urcap_tutorial_swing.pdf &>/dev/null; then
    echo "download error."
    return 1
  fi
  echo "done!"

  build_ur_image "${ur_sim_path}" "${ur_sdk_path}" "it.unibz.smf" || exit 1

  rm -rf "${ur_sim_path}" "${ur_sdk_path}"
}

# validate the execution directory
scriptdir=$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)
if [ "$scriptdir" != "$(pwd)" ]; then
  echo "this script must be executed from its containing directory".
  exit 1
fi

setup_ur_tools
exit 0
