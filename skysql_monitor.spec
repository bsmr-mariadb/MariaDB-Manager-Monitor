%define _topdir	 	%(echo $PWD)/
%define name		skysql_monitor
%define release		##RELEASE_TAG##
%define version 	##VERSION_TAG##
%define buildroot 	%{_topdir}/%{name}-%{version}-%{release}-root
%define install_path	/usr/local/skysql/monitor/

BuildRoot:		%{buildroot}
BuildArch:              noarch
Summary: 		SkySQL monitor
License: 		GPL
Name: 			%{name}
Version: 		%{version}
Release: 		%{release}
Source: 		%{name}-%{version}-%{release}.tar.gz
Prefix: 		/
Group: 			Development/Tools
Requires:		java-1.7.0-openjdk, rsyslog
#BuildRequires:		java-1.7.0-openjdk

%description
SkySQL monitor

%prep

%setup -q

%build

%post

%install
mkdir -p $RPM_BUILD_ROOT%{install_path}
cp ClusterMonitor.jar $RPM_BUILD_ROOT%{install_path}
cp skysql-monitor.sh $RPM_BUILD_ROOT%{install_path}
mkdir -p $RPM_BUILD_ROOT%/etc/init.d/
cp mariadb-enterprise-monitor $RPM_BUILD_ROOT%/etc/init.d/

%clean

%files
%defattr(-,root,root)
%{install_path}
%{install_path}ClusterMonitor.jar
%{install_path}skysql-monitor.sh

%changelog

* Thu Oct 10 2013 Massimo Siani <massimo.siani@skysql.com>
- Add init script

* Mon Oct 7 2013 Massimo Siani <massimo.siani@skysql.com>
- When a node is deleted, also remove its cached data

* Fri Oct 4 2013 Massimo Siani <massimo.siani@skysql.com>
- Fix a bug when retrieve node credentials
- Fix a bug when caching node data

* Tue Oct 1 2013 Massimo Siani <massimo.siani@skysql.com>
- Bug fix
- Fix .spec file

* Mon Sep 30 2013 Massimo Siani <massimo.siani@skysql.com>
- Add Galera nodes membership.
- Switch to syslog.
- Add provisioned node API support.
- Refresh based on If-Modified-Since.

* Tue Sep 10 2013 Massimo Siani <massimo.siani@skysql.com>
- Fixed node state API incompatibility.
- Removed json simple, and switched to JSON objects only.

* Thu Sep 5 2013 Massimo Siani <massimo.siani@skysql.com>
- Remove any reference to SQLite db file.

* Wed Sep 4 2013 Massimo Siani <massimo.siani@skysql.com>
- Implemented Gson

* Tue Sep 3 2013 Massimo Siani <massimo.siani@skysql.com>
- Fixings

* Thu Aug 29 2013 Massimo Siani <massimo.siani@skysql.com> - 1.6.0
- Added: GET API calls compatible with new API

* Fri Aug 16 2013 Massimo Siani <massimo.siani@skysql.com> - 1.5.3
- Fixed: jdbc connection hangs on a node shutdown

* Fri Aug 2 2013 Massimo Siani <massimo.siani@skysql.com> - 1.5.2
- Added: no quit if no systems and/or nodes found
- Added: refresh rate set to 10 seconds until at least one node is defined
- Added: methods for JavaScript calls
- Fixed: if the connection hangs, close it and reconnect
- Removed Amazon unused library, size decreased to less than 1MB

* Wed Aug 1 2013 Massimo Siani <massimo.siani@skysql.com> - 1.5.1
- Fixed various bugs
- Use API
- Keep looking for nodes if systems exist
- Added buffering
- Added support for batch execution
- Json parsing & Gson interface added
- Code cleanup

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
