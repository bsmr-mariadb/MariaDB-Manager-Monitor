%define _topdir	 	%(echo $PWD)/
%define name		skysql_monitor
%define release		2
%define version 	0.1
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
Requires:		java-1.6.0-openjdk aws-apitools-ec2 skysql_aws_tools mariadb-java-client sqlite-jdbc aws-java-sdk
BuildRequires:		java-1.6.0-openjdk skysql_aws_tools sqlite-jdbc aws-java-sdk mariadb-java-client

%description
SkySQL monitor

%prep

%setup -q

%build

javac -d . -classpath `find /usr/local/skysql/skysql_aws/*.jar | tr '\n' ':' `  `find src/com/skysql/monitor | grep \.java`
jar cf ClusterMonitor.jar com/skysql/monitor/*.class

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
* Wed Apr 03 2013 Timofey Turenko <timofey.turenko@skysql.com> - 0.1-2
- first packaged version

