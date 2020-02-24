buildNumber=$(<buildNumber.txt)
((buildNumber++))
./gradlew publish -PideaMajor=2019 -PideaMinor=3 -PideaSinceBuild=193.2956 -PideaUntilBuild=193.* -PisPublish=true "-PbuildNumber=${buildNumber}"
./gradlew publish -PideaMajor=2019 -PideaMinor=2 -PideaSinceBuild=192.2840 -PideaUntilBuild=192.* -PisPublish=true "-PbuildNumber=${buildNumber}"
echo "${buildNumber}" > buildNumber.txt
