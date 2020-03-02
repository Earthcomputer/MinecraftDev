buildNumber=$(<buildNumber.txt)
((buildNumber++))
./gradlew publish -PideaMajor=2020 -PideaMinor=1 -PideaSinceBuild=201.2803 -PideaUntilBuild=201.* -PtomlVersion=0.2.114.35-193 -PgradleToolingExtensionVersion=201.3803.71-EAP-SNAPSHOT -PisPublish=true "-PbuildNumber=${buildNumber}"
./gradlew publish -PideaMajor=2019 -PideaMinor=3 -PideaSinceBuild=193.2956 -PideaUntilBuild=193.* -PtomlVersion=0.2.111.34-193 -PgradleToolingExtensionVersion=193.5233.102 -PisPublish=true "-PbuildNumber=${buildNumber}" -PideaVersion=2019.3
./gradlew publish -PideaMajor=2019 -PideaMinor=2 -PideaSinceBuild=192.2840 -PideaUntilBuild=192.* -PtomlVersion=0.2.0.25 -PgradleToolingExtensionVersion=193.5233.102 -PisPublish=true "-PbuildNumber=${buildNumber}" -PideaVersion=2019.2
echo "${buildNumber}" > buildNumber.txt
