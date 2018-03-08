# shiro-redisson

[![Travis](https://img.shields.io/travis/streamone/shiro-redisson.svg)](https://travis-ci.org/streamone/shiro-redisson)
[![Coverage Status](https://coveralls.io/repos/github/streamone/shiro-redisson/badge.svg?branch=master)](https://coveralls.io/github/streamone/shiro-redisson?branch=master)
[![license](https://img.shields.io/badge/license-MIT%20License-blue.svg)](https://github.com/streamone/shiro-redisson/blob/master/LICENSE)

### Redis based implementations of Apache Shiro's Cache and Session, using [redisson](https://github.com/redisson/redisson) as rich redis client.

compatible with:
* shiro 1.3.x
* redisson 2.x
* redis 4.x

### [Documentation](https://github.com/streamone/shiro-redisson/wiki)

### Features
* Shiro Cache implementation
  * support cache config according to Spring Cache specification
  * support several serialization solutions: JDK Serialization, JSON, Smile, MsgPack, FST and so on
  * support common redis servers mode,  including single server, master-salve replication, sentinel, cluster

* Shiro Session implementation
  * serialize attributes of session individually as minimum unit
  * package redis commands in lua script for high performance (reduce round-trip time)  and supporting transaction
  * support several serialization solutions: JDK Serialization, JSON, Smile, MsgPack, FST and so on
  * support common redis servers mode,  including single server, master-salve replication, sentinel, cluster