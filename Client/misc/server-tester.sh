#!/bin/bash

remote_server="167.99.136.45"
remote_port="8000"

for i in {0..200}; do
  nc "$remote_server" "$remote_port" &
  sleep 0.01
done

# Wait for all child processes to finish
wait
