#takes single argument of streamName
gradle clean dist;
yes All | unzip build/distributions/*.zip;
./koupler-0.2.14-SNAPSHOT/koupler.sh -udp -streamName "$1";
