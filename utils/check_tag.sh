#!/bin/bash

# $1 = current version number
# $2 = upstream openjdk version number
# $3 = java home
curr_version=$1
openjdk_version=$2
java_home=$3

err_exit()
{
  exit_msg=$1
  echo ${exit_msg}
  exit 1
}

if [ -z "$curr_version" ];then
  err_exit "no input version!"
elif [ -z "$openjdk_version" ];then
  err_exit "no input openjdk version!"
elif [ -z "$java_home" ];then
  if [ -n "$JAVA_HOME" ];then
    java_home=$JAVA_HOME
  else
    err_exit "no input java_home!"
  fi
fi

if [ ! -f "${java_home}/bin/java" ];then
  err_exit "invalid java_home!"
fi
java_bin=${java_home}/bin/java

if [ -z "`echo ${curr_version} | grep -E '^[1-9][0-9]*((\.0)*\.[1-9][0-9]*)*\+(0|[1-9][0-9]*)$'`" ];then
  err_exit "invalid version!"
fi

upstream_tag=`curl -s -H "Accept: application/vnd.github.v3+json" https://api.github.com/repos/adoptium/temurin${openjdk_version}-binaries/releases/latest | grep tag_name | cut -d ':' -f 2 | cut -d '"' -f 2`
upstream_tag=`echo ${upstream_tag#*'jdk-'} | sed 's/+/./g'`

java_version_info=`${java_bin} -version 2>&1`
java_version=`echo ${java_version_info##*'build '} | cut -d ',' -f 1 | sed 's/+/./g'`

old_IFS=$IFS
IFS=$'.'
curr_list=(${curr_version})
upstream_list=(${upstream_tag})
java_list=(${java_version})
if [ "${#curr_list[@]}" -lt "${#java_list[@]}" ];then
  err_exit "invalid version!"
fi

flag=0
for((idx=0; idx<${#java_list[@]}; idx++))
do
  ups_num=`eval echo '$'"{upstream_list[$idx]}"`
  java_num=`eval echo '$'"{java_list[$idx]}"`
  curr_num=`eval echo '$'"{curr_list[$idx]}"`
  if [ "$idx" -lt "${#upstream_list[@]}" ] && [ "${ups_num}" != "${curr_num}" ];then
    err_exit "invalid version!"
  fi
  if [ "$flag" -eq 0 ] && [ "${curr_num}" -lt "${java_num}" ];then
    err_exit "invalid version!"
  fi
  if [ "${curr_num}" -gt "${java_num}" ];then
    flag=1
  fi
done
if [ "$flag" -eq 0 ] && [ "${#curr_list[@]}" -eq "${#java_list[@]}" ];then
  err_exit "invalid version!"
fi

sys=`uname -a | grep -i cygwin`
res=0
if [ -n "$sys" ];then
  # system is windows
  if [ -z "`echo ${java_version_info} | grep 'OpenJDK 32-Bit'`" ];then
    echo "Only 32-bit is supported under Windows system"
  else
    let res++
  fi
else
  if [ -z "`echo ${java_version_info} | grep 'OpenJDK 64-Bit'`" ];then
    echo "Only 64-bit is supported on non Windows systems"
  else
    let res++
  fi
fi
if [ -z "`echo ${java_version_info} | grep 'Alibaba Dragonwell'`" ];then
  echo "not dragonwell jdk"
else
  let res++
fi
if [ "$res" -lt 2 ];then
  exit 1
fi