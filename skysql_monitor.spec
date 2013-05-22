%define _topdir	 	%(echo $PWD)/
%define name		skysql_monitor
%define release		2
%define version 	1.4
%define buildroot %{_topdir}/%{name}-%{version}-%{release}-root
%define install_path	/usr/local/skysql/monitor/

BuildRoot:	%{buildroot}
Summary: 		SkySQL monitor
License: 		GPL
Name: 			%{name}
Version: 		%{version}
Release: 		%{release}
Source: 		%{name}-%{version}-%{release}.tar.gz
Prefix: 		/
Group: 			Development/Tools
Requires:		java-1.6.0-openjdk skysql_aws_tools mariadb-java-client sqlite-jdbc aws-java-sdk
# aws-apitools-ec2
BuildRequires:		java-1.6.0-openjdk skysql_aws_tools sqlite-jdbc aws-java-sdk mariadb-java-client

%description
SkySQL monitor

%prep

%setup -q

%build

#javac -d . -classpath `find /usr/local/skysql/skysql_aws/*.jar | tr '\n' ':' `  `find src/com/skysql/monitor | grep \.java`
#jar cf ClusterMonitor.jar com/skysql/monitor/*.class

%post
touch /var/log/SkySQL-ClusterMonitor.log
chown apache:apache /var/log/SkySQL-ClusterMonitor.log

%install
mkdir -p $RPM_BUILD_ROOT%{install_path}
cp ClusterMonitor.jar  $RPM_BUILD_ROOT%{install_path}
cp monitor/ClusterMonitor.sh  $RPM_BUILD_ROOT%{install_path}

%clean

%files
%defattr(-,root,root)
%{install_path}
%{install_path}ClusterMonitor.jar
%{install_path}ClusterMonitor.sh

%changelog
* Wed May 22 2013 Timofey Turenko <timofey.turenko@skysql.com> - 1.4-2
- add sleep for 10 seconds before trying again to get systems

* Wed May 15 2013 Mark Riddoch <mark.riddoch@skysql.com> - 1.4-1
- Fix for clash with RestEasy and the AWS API.
- Updated CRM monitor and SetNodeState to use the new API
* Wed May 01 2013 Mark Riddoch <mark.riddoch@skysql.com> - 1.4-0
- Use the SkySQL Management REST interface for adding monitor values
- There are three Java Properties that must now be set, SKYSQL_API_HOST,
- SKYSQL_API_KEY, SKYSQL_API_KEYID
* Wed Apr 17 2013 Mark Riddoch <mark.riddoch@skysql.com> - 1.3-1
- Addition of SQL_NODE_STATE monitor type
- Fix for a bug that caused delta monitors to go negative after a failure for the server to respond
* Wed Apr 03 2013 Timofey Turenko <timofey.turenko@skysql.com> - 0.1-2
- first packaged version
