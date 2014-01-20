MariaDB-Manager-Monitor
=======================

MariaDB-Manager-Monitor is the monitoring component of the MariaDB-Manager project. It provides a confirgurable monitoring service in Java that interacts with the MariaDB-Manager-API to provide monitoring data on a set of servers within a MariaDB Galera Cluster.

The monitor requires another component of the MariaDB-Manager family in order to be built, libMariaDB-Manager-java. This project provides library functions that are shared between the Java components of MariaDB-Manager.

Other components within the MariaDB-Manager family are

| Project                   | Description                                                                |
|---------------------------|----------------------------------------------------------------------------|
| MariaDB-Manager           | The overall project that encapsualtes all the components of the MariaDB-Manager|
| MariaDB-Manager-API       | The REST API that gives access to all MariaDB-Manager monitoring and management features|
| MariaDB-Manager-GREX      | The remotely executed components of the MariaDB-Manager that are installed on every node in the clsuter|
| MariaDB-Manager-WebUI     | The web user interface for the MariaDB-Manager                             |

