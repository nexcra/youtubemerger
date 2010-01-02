Running code from the command line:

Prerequisites Java and Maven 2

From the project folder:

export XUGGLE_HOME=/usr/local/xuggler                         #Where you have installed Xuggler
export DYLD_LIBRARY_PATH=$XUGGLE_HOME/lib:$DYLD_LIBRARY_PATH
export PATH=$XUGGLE_HOME/bin:$PATH

mvn clean install exec:java -Dexec.mainClass="video.FlvWriter"