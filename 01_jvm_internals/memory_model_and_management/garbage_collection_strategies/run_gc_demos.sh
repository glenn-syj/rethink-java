#!/bin/bash

# This script compiles and runs the GC demo applications with various GC algorithms
# and generates GC log files for analysis.
#
# IMPORTANT: This script should be run from the 'rethink-java/' project root directory.
# Example: ./01_jvm_internals/memory_model_and_management/garbage_collection_strategies/run_gc_demos.sh

# --- Configuration ---
DEMO_DIR="./01_jvm_internals/memory_model_and_management/garbage_collection_strategies"
SHORT_LIVED_DEMO="memory_model_and_management.garbage_collection_strategies.ShortLivedObjectDemo"
LONG_LIVED_DEMO="memory_model_and_management.garbage_collection_strategies.LongLivedObjectDemo"
LOG_DIR="$DEMO_DIR/gc_logs"

# Ensure the log directory exists
mkdir -p "$LOG_DIR"

# Common JVM options for heap size (can be adjusted)
# Limiting heap to make GC events more frequent for observation
HEAP_OPTIONS="-Xms256m -Xmx256m"

# Control variables for GC algorithm execution
# Set to "true" to run the respective GC, "false" to skip.
RUN_SERIAL_GC="false"
RUN_PARALLEL_GC="false"
RUN_G1_GC="true"
RUN_ZGC="false" # ZGC is experimental and may require specific JDK versions/configurations.

# --- Functions ---

# Function to clean up compiled classes and log files
clean() {
    echo "Cleaning up .class files and GC logs..."
    find "$DEMO_DIR" -name "*.class" -delete
    rm -rf "$LOG_DIR"
    mkdir -p "$LOG_DIR"
    echo "Cleanup complete."
}

# Function to compile Java demo files
compile_demos() {
    echo "Compiling Java demo files..."
    # Compile from project root, placing .class files in correct package structure
    javac -d . "$DEMO_DIR/ShortLivedObjectDemo.java"
    javac -d . "$DEMO_DIR/LongLivedObjectDemo.java"
    if [ $? -eq 0 ]; then
        echo "Compilation successful."
    else
        echo "Compilation failed. Exiting."
        exit 1
    fi
}

# Function to run ShortLivedObjectDemo with different GCs
run_short_lived_demo() {
    local demo_name="$SHORT_LIVED_DEMO"
    echo "--- Running ShortLivedObjectDemo with various GCs ---"

    if [ "$RUN_SERIAL_GC" = "true" ]; then
        # Serial GC
        echo "Running with Serial GC..."
        java $HEAP_OPTIONS -XX:+UseSerialGC -Xlog:gc*=info:file="$LOG_DIR/shortlived_serial_gc.log" -classpath . "$demo_name" &
        PID=$!
        sleep 10 # Let it run for a while to generate logs
        kill $PID > /dev/null 2>&1
        echo "Serial GC run complete. Log saved to $LOG_DIR/shortlived_serial_gc.log"
    else
        echo "Skipping ShortLivedObjectDemo with Serial GC."
    fi

    if [ "$RUN_PARALLEL_GC" = "true" ]; then
        # Parallel GC
        echo "Running with Parallel GC..."
        java $HEAP_OPTIONS -XX:+UseParallelGC -Xlog:gc*=info:file="$LOG_DIR/shortlived_parallel_gc.log" -classpath . "$demo_name" &
        PID=$!
        sleep 10
        kill $PID > /dev/null 2>&1
        echo "Parallel GC run complete. Log saved to $LOG_DIR/shortlived_parallel_gc.log"
    else
        echo "Skipping ShortLivedObjectDemo with Parallel GC."
    fi

    if [ "$RUN_G1_GC" = "true" ]; then
        # G1 GC
        echo "Running with G1 GC..."
        java $HEAP_OPTIONS -XX:+UseG1GC -Xlog:gc*=info:file="$LOG_DIR/shortlived_g1_gc.log" -classpath . "$demo_name" &
        PID=$!
        sleep 10
        kill $PID > /dev/null 2>&1
        echo "G1 GC run complete. Log saved to $LOG_DIR/shortlived_g1_gc.log"
    else
        echo "Skipping ShortLivedObjectDemo with G1 GC."
    fi

    if [ "$RUN_ZGC" = "true" ]; then
        # ZGC (requires JDK 11+ and specific JVM configuration/build if not default)
        # Check if ZGC is supported before running
        if java -XX:+UnlockExperimentalVMOptions -XX:+UseZGC -version > /dev/null 2>&1; then
            echo "Running with ZGC (experimental)..."
            java $HEAP_OPTIONS -XX:+UnlockExperimentalVMOptions -XX:+UseZGC -Xlog:gc*=info:file="$LOG_DIR/shortlived_zgc_gc.log" -classpath . "$demo_name" &
            PID=$!
            sleep 10
            kill $PID > /dev/null 2>&1
            echo "ZGC run complete. Log saved to $LOG_DIR/shortlived_zgc_gc.log"
        else
            echo "Skipping ZGC: Not supported or experimental options not unlocked."
        fi
    else
        echo "Skipping ShortLivedObjectDemo with ZGC."
    fi

    echo "--- ShortLivedObjectDemo GC comparison runs finished ---"
}

# Function to run LongLivedObjectDemo with different GCs
run_long_lived_demo() {
    local demo_name="$LONG_LIVED_DEMO"
    echo "--- Running LongLivedObjectDemo with various GCs ---"

    if [ "$RUN_SERIAL_GC" = "true" ]; then
        # Serial GC
        echo "Running with Serial GC..."
        java $HEAP_OPTIONS -XX:+UseSerialGC -Xlog:gc*=info:file="$LOG_DIR/longlived_serial_gc.log" -classpath . "$demo_name" &
        PID=$!
        sleep 15 # Longer run time to ensure Full GC
        kill $PID > /dev/null 2>&1
        echo "Serial GC run complete. Log saved to $LOG_DIR/longlived_serial_gc.log"
    else
        echo "Skipping LongLivedObjectDemo with Serial GC."
    fi

    if [ "$RUN_PARALLEL_GC" = "true" ]; then
        # Parallel GC
        echo "Running with Parallel GC..."
        java $HEAP_OPTIONS -XX:+UseParallelGC -Xlog:gc*=info:file="$LOG_DIR/longlived_parallel_gc.log" -classpath . "$demo_name" &
        PID=$!
        sleep 15
        kill $PID > /dev/null 2>&1
        echo "Parallel GC run complete. Log saved to $LOG_DIR/longlived_parallel_gc.log"
    else
        echo "Skipping LongLivedObjectDemo with Parallel GC."
    fi

    if [ "$RUN_G1_GC" = "true" ]; then
        # G1 GC
        echo "Running with G1 GC..."
        java $HEAP_OPTIONS -XX:+UseG1GC -Xlog:gc*=info:file="$LOG_DIR/longlived_g1_gc.log" -classpath . "$demo_name" &
        PID=$!
        sleep 15
        kill $PID > /dev/null 2>&1
        echo "G1 GC run complete. Log saved to $LOG_DIR/longlived_g1_gc.log"
    else
        echo "Skipping LongLivedObjectDemo with G1 GC."
    fi

    if [ "$RUN_ZGC" = "true" ]; then
        # ZGC (requires JDK 11+ and specific JVM configuration/build if not default)
        if java -XX:+UnlockExperimentalVMOptions -XX:+UseZGC -version > /dev/null 2>&1; then
            echo "Running with ZGC (experimental)..."
            java $HEAP_OPTIONS -XX:+UnlockExperimentalVMOptions -XX:+UseZGC -Xlog:gc*=info:file="$LOG_DIR/longlived_zgc_gc.log" -classpath . "$demo_name" &
            PID=$!
            sleep 15
            kill $PID > /dev/null 2>&1
            echo "ZGC run complete. Log saved to $LOG_DIR/longlived_zgc_gc.log"
        else
            echo "Skipping ZGC: Not supported or experimental options not unlocked."
        fi
    else
        echo "Skipping LongLivedObjectDemo with ZGC."
    fi

    echo "--- LongLivedObjectDemo GC comparison runs finished ---"
}

# --- Main Script Execution ---
echo "Starting GC Demos execution script..."

clean
compile_demos

run_short_lived_demo
run_long_lived_demo

echo "All GC Demos execution finished. Check '$LOG_DIR' for GC logs."
echo "You can analyze these logs using tools like GCViewer or GCEasy."
