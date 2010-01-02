Running code from the command line:

Prerequisites Java and Maven 2

From the project folder:

export XUGGLE_HOME=/usr/local/xuggler                         #Where you have installed Xuggler to
export DYLD_LIBRARY_PATH=$XUGGLE_HOME/lib:$DYLD_LIBRARY_PATH
export PATH=$XUGGLE_HOME/bin:$PATH

mvn exec:java -Dexec.mainClass="video.FlvWriter" -e      #-e is for viewing exceptions and log messages