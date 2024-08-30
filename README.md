# Mouse Idle Stats

Simple tool to check when mouse is idle and count the time.

## Main objective

During my diagnosis of ADHD I felt the necessity to monitor how much time I spent with other activities that could harm my performance at work.
This tool was usefully to know how much time I spent far from the screen.

The script will start to count time when mouse is stopped for more than 2 minutes.

### How to build?

Pre-requisites:
* JDK 17+

At terminal run the script (if needed give execution permission to the file):
```shell
./generate-jar.sh
```

This script will generate `mouseIdleStats.jar` file into `dist` folder.

### How to run?

Run the jar.

```shell
java -jar dist/mouseIdleStats.jar
```

#### Keep OS alive option

There is an option to keep OS alive during checking mouse idle. This can be usefully to prevent the computer to sleep.

```shell
java -jar dist/mouseIdleStats.jar --keep-os-alive`
```

### Future improvements

* Configurable mouse check interval
* Check also keyboard and other kind of user interactions.