#!/bin/bash
# This script creates a tunnel to localhost:3002 using serveo.net
# Compare https://codevelopergptengine.stoerr.net/https.html for setup instructions
# If placed in ~/.cgptcodeveloperglobal/tunnel.sh it will automatically be started with the codeveloperengine script
# Take a random customized URL like cuslwem2357sdlcs7tlsd.serveo.net

# The custom URL to use for the tunnel
CUSTOM_URL="YOUROWNURL_CUSTOMIZE_THIS.serveo.net"
# Time in seconds after which the script will exit
AUTOKILLTIME=10800

# Function to start ssh
start_ssh() {
    ssh -T -R $CUSTOM_URL:80:localhost:3002 serveo.net &
    sshpid=$!
}

# Function to handle exit
on_exit() {
    echo "Exiting..."
    kill $sshpid 2>/dev/null
    exit
}

# Function to handle interruptions
on_interrupt() {
    echo "Interrupted by user. Terminating..."
    kill $sshpid 2>/dev/null
    exit
}

# Trap different signals
trap on_exit EXIT
trap on_interrupt INT

# Start ssh
start_time=$(date +%s)
start_ssh

# Monitor ssh process and time
while true; do
    # Check if the SSH process is still running
    if ps -p $sshpid > /dev/null; then
        # SSH process is running
        # Check if AUTOKILLTIME has passed
        current_time=$(date +%s)
        elapsed_time=$(( current_time - start_time ))
        if [ $elapsed_time -ge $AUTOKILLTIME ]; then
                echo "Script has been running for $AUTOKILLTIME seconds. Exiting..."
            kill $sshpid 2>/dev/null
            exit
        fi
        sleep 60
    else
        # SSH process has exited
        wait $sshpid
        ssh_status=$?
        if [ $ssh_status -ne 0 ]; then
            echo "SSH process ended unexpectedly with status $ssh_status. Restarting after 600s..."

            # Check if AUTOKILLTIME has passed before restarting
            current_time=$(date +%s)
            elapsed_time=$(( current_time - start_time ))
            if [ $elapsed_time -ge $AUTOKILLTIME ]; then
                echo "Script has been running for $AUTOKILLTIME seconds. Exiting..."
                exit
            fi

            sleep 600
            start_ssh
        else
            echo "SSH process ended normally. Exiting..."
            exit
        fi
    fi
done
